package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;

import java.util.List;

public interface DishOptionService {
    DishOptionResponseDto createDishOption(DishOptionRequestDto request);
    List<DishOptionResponseDto> getAllDishOptions();
    List<DishOptionResponseDto> getDishOptionsByGroupId(Long optionGroupId);
    DishOptionResponseDto getDishOptionById(Long id);
    DishOptionResponseDto updateDishOption(Long id, DishOptionRequestDto request);
    void deleteDishOption(Long id);
}
