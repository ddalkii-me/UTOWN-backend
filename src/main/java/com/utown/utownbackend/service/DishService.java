package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.DishStatus;
import java.util.List;

public interface DishService {

    DishResponseDto createDish(DishRequestDto request);

    List<DishResponseDto> getDishes(DishStatus status, boolean deleted);

    List<DishResponseDto> getDishes(Long restaurantId, Long categoryId, DishStatus status, boolean deleted);

    DishResponseDto getDishById(Long id);

    DishResponseDto updateDish(Long id, DishRequestDto request);

    void deleteDish(Long id);

    void restoreDish(Long id);
}

