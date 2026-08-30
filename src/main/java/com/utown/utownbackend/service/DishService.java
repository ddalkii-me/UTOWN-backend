package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import java.util.List;

public interface DishService {

    DishResponseDto createDish(DishRequestDto request);

    List<DishResponseDto> getAllDishes();

    DishResponseDto getDishById(Long id);

    DishResponseDto updateDish(Long id, DishRequestDto request);

    void deleteDish(Long id);
}

