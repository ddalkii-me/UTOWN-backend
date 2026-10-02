package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.dto.CategoryResponseDto;
import com.utown.utownbackend.entity.Category;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.repository.CategoryRepository;
import com.utown.utownbackend.repository.DishRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.exception.ResourceConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.CacheManager;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final CacheManager cacheManager;

    @Transactional
    @Override
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
        log.info("Executing createCategory");

        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        Category category = new Category();
        category.setRestaurant(restaurant);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setPriority(request.priority());

        Category savedCategory = categoryRepository.save(category);

        evictCacheEntry("categoriesByRestaurant", "all");
        evictCacheEntry("categoriesByRestaurant", request.restaurantId());
        evictCacheEntry("restaurantMenus", request.restaurantId());

        return new CategoryResponseDto(
                savedCategory.getId(),
                savedCategory.getRestaurant().getId(),
                savedCategory.getName(),
                savedCategory.getDescription(),
                savedCategory.getImageUrl(),
                savedCategory.getPriority()
        );
    }

    @Cacheable(cacheNames = "categoriesByRestaurant", key = "'all'")
    @Override
    public List<CategoryResponseDto> getAllCategories() {
        log.info("Executing getAllCategories");
        return getAllCategories(null);
    }

    @Cacheable(cacheNames = "categoriesByRestaurant", key = "#p0 == null ? 'all' : #p0")
    @Override
    public List<CategoryResponseDto> getAllCategories(Long restaurantId) {
        log.info("Executing getAllCategories with restaurantId={}", restaurantId);
        List<Category> categories;
        if (restaurantId != null) {
            categories = categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId);
        } else {
            categories = categoryRepository.findAllByDeletedAtIsNull();
        }

        return categories.stream()
                .map(category -> new CategoryResponseDto(
                        category.getId(),
                        category.getRestaurant().getId(),
                        category.getName(),
                        category.getDescription(),
                        category.getImageUrl(),
                        category.getPriority()
                ))
                .toList();
    }

    @Cacheable(cacheNames = "categoriesById", key = "#p0")
    @Override
    public CategoryResponseDto getCategoryById(Long id) {
        log.info("Executing getCategoryById with id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        return new CategoryResponseDto(
                category.getId(),
                category.getRestaurant().getId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getPriority()
        );
    }

    @Transactional
    @Override
    public CategoryResponseDto updateCategory(
            Long id,
            CategoryRequestDto request) {
        log.info("Executing updateCategory with id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Long previousRestaurantId = category.getRestaurant().getId();

        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        category.setRestaurant(restaurant);
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setPriority(request.priority());

        Category updatedCategory = categoryRepository.save(category);

        evictCacheEntry("categoriesByRestaurant", "all");
        evictCacheEntry("categoriesByRestaurant", previousRestaurantId);
        evictCacheEntry("categoriesByRestaurant", restaurant.getId());
        evictCacheEntry("categoriesById", id);
        evictCacheEntry("restaurantMenus", previousRestaurantId);
        evictCacheEntry("restaurantMenus", restaurant.getId());

        return new CategoryResponseDto(
                updatedCategory.getId(),
                updatedCategory.getRestaurant().getId(),
                updatedCategory.getName(),
                updatedCategory.getDescription(),
                updatedCategory.getImageUrl(),
                updatedCategory.getPriority()
        );
    }

    @Transactional
    @Override
    public void deleteCategory(Long id) {
        log.info("Executing deleteCategory with id={}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Long restaurantId = category.getRestaurant().getId();

        if (dishRepository.existsByCategoryIdAndDeletedAtIsNull(id)) {
            throw new ResourceConflictException("Cannot delete category because it still has active dishes.");
        }

        category.setDeletedAt(LocalDateTime.now());

        categoryRepository.save(category);

        evictCacheEntry("categoriesByRestaurant", "all");
        evictCacheEntry("categoriesByRestaurant", restaurantId);
        evictCacheEntry("categoriesById", id);
        evictCacheEntry("restaurantMenus", restaurantId);
    }

    private void evictCacheEntry(String cacheName, Object key) {
        if (key == null) {
            return;
        }
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}