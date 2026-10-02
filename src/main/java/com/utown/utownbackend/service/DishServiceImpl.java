package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.Category;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.repository.CategoryRepository;
import com.utown.utownbackend.repository.DishRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    final DishRepository dishRepository;
    final RestaurantRepository restaurantRepository;
    final CategoryRepository categoryRepository;
    final com.utown.utownbackend.repository.DishOptionGroupRepository dishOptionGroupRepository;
    final FileStorageService fileStorageService;
    final DishImageUpdateService dishImageUpdateService;

    public DishServiceImpl(DishRepository dishRepository, RestaurantRepository restaurantRepository, CategoryRepository categoryRepository, com.utown.utownbackend.repository.DishOptionGroupRepository dishOptionGroupRepository, FileStorageService fileStorageService,
                           DishImageUpdateService dishImageUpdateService) {
        this.dishRepository = dishRepository;
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.dishOptionGroupRepository = dishOptionGroupRepository;
        this.fileStorageService = fileStorageService;
        this.dishImageUpdateService = dishImageUpdateService;
    }

    @Override
    @Transactional
    public DishResponseDto createDish(DishRequestDto request) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId()).orElseThrow(
                () -> new EntityNotFoundException("Restaurant not found")
        );

        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(
                () -> new EntityNotFoundException("Category not found")
        );

        if (!category.getRestaurant().getId().equals(restaurant.getId())) {
            throw new IllegalArgumentException("Category does not belong to the specified restaurant");
        }

        Dish dish = new Dish();
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setName(request.name());
        dish.setPrice(request.price());
        dish.setDescription(request.description());
        dish.setImageUrl(request.imageUrl());
        dish.setStatus(request.status());
        dish.setSortOrder(request.sortOrder());

        Dish savedDish = dishRepository.save(dish);


        return toDto(savedDish);
    }

    @Override
    public List<DishResponseDto> getDishes(DishStatus status, boolean deleted) {
        return getDishes(null, null, status, deleted);
    }

    @Override
    public List<DishResponseDto> getDishes(Long restaurantId, Long categoryId, DishStatus status, boolean deleted) {
        List<Dish> dishes;
        if (deleted) {
            dishes = dishRepository.findAllByDeletedAtIsNotNull();
        } else if (restaurantId != null && categoryId != null && status != null) {
            dishes = dishRepository.findAllByRestaurantIdAndCategoryIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId, categoryId, status);
        } else if (restaurantId != null && categoryId != null) {
            dishes = dishRepository.findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId, categoryId);
        } else if (restaurantId != null && status != null) {
            dishes = dishRepository.findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId, status);
        } else if (restaurantId != null) {
            dishes = dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId);
        } else if (categoryId != null) {
            dishes = dishRepository.findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(categoryId);
        } else if (status != null) {
            dishes = dishRepository.findAllByStatusAndDeletedAtIsNull(status);
        } else {
            dishes = dishRepository.findAllByDeletedAtIsNull();
        }
        return dishes.stream().map(this::toDto).toList();
    }

    @Override
    public DishResponseDto getDishById(Long id) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));

        return toDto(dish);
    }


    @Override
    @Transactional
    public DishResponseDto updateDish(Long id, DishRequestDto request) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId()).orElseThrow(
                () -> new EntityNotFoundException("Restaurant not found")
        );

        Category category = categoryRepository.findById(request.categoryId()).orElseThrow(
                () -> new EntityNotFoundException("Category not found")
        );

        if (!category.getRestaurant().getId().equals(restaurant.getId())) {
            throw new IllegalArgumentException("Category does not belong to the specified restaurant");
        }
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setName(request.name());
        dish.setPrice(request.price());
        dish.setDescription(request.description());
        dish.setImageUrl(request.imageUrl());
        dish.setStatus(request.status());
        dish.setSortOrder(request.sortOrder());

        Dish savedDish = dishRepository.save(dish);


        return toDto(savedDish);
    }

    @Override
    @Transactional
    public void deleteDish(Long id) {
        if (dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(id)) {
            throw new IllegalStateException("Cannot delete Dish while it has active Option Groups");
        }

        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(
                () -> new EntityNotFoundException("Dish not found")
        );
        dish.setDeletedAt(LocalDateTime.now());
        dishRepository.save(dish);
    }

    @Override
    @Transactional
    public void restoreDish(Long id) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNotNull(id).orElseThrow(
                () -> new EntityNotFoundException("Deleted dish not found")
        );
        dish.setDeletedAt(null);
        dishRepository.save(dish);
    }

    @Override
    public DishResponseDto uploadDishImage(Long id, MultipartFile file) throws IOException {
        String imageKey = fileStorageService.uploadFile(file);

        return dishImageUpdateService.updateImageUrl(id, imageKey);
    }

    private DishResponseDto toDto(Dish dish) {
        return new DishResponseDto(
                dish.getId(),
                dish.getRestaurant().getId(),
                dish.getCategory().getId(),
                dish.getName(),
                dish.getPrice(),
                dish.getDescription(),
                dish.getImageUrl(),
                dish.getStatus(),
                dish.getSortOrder(),
                dish.getDeletedAt()
        );
    }

}
