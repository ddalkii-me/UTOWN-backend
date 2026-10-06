package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.repository.DishRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.CacheManager;

@Service
public class DishImageUpdateService {

    private final DishRepository dishRepository;
    private final CacheManager cacheManager;

    public DishImageUpdateService(DishRepository dishRepository, CacheManager cacheManager) {
        this.dishRepository = dishRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public DishResponseDto updateImageUrl(Long id, String imageKey) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Dish not found")
                );

        dish.setImageUrl(imageKey);

        Dish savedDish = dishRepository.save(dish);

        var menuCache = cacheManager.getCache("restaurantMenus");
        if (menuCache != null) {
            menuCache.evict(savedDish.getRestaurant().getId());
        }

        return new DishResponseDto(
                savedDish.getId(),
                savedDish.getRestaurant().getId(),
                savedDish.getCategory().getId(),
                savedDish.getName(),
                savedDish.getPrice(),
                savedDish.getDescription(),
                savedDish.getImageUrl(),
                savedDish.getStatus(),
                savedDish.getSortOrder(),
                savedDish.getDeletedAt()
        );
    }
}
