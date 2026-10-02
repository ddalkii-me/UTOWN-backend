package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.repository.DishRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishImageUpdateService {

    private final DishRepository dishRepository;

    public DishImageUpdateService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @Transactional
    public DishResponseDto updateImageUrl(Long id, String imageKey) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Dish not found")
                );

        dish.setImageUrl(imageKey);

        Dish savedDish = dishRepository.save(dish);

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
