package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;

import java.util.List;

public interface DishOptionGroupService {
    DishOptionGroupResponseDto createDishOptionGroup(DishOptionGroupRequestDto request);
    List<DishOptionGroupResponseDto> getAllDishOptionGroups();
    List<DishOptionGroupResponseDto> getDishOptionGroupsByDishId(Long dishId);
    DishOptionGroupResponseDto getDishOptionGroupById(Long id);
    DishOptionGroupResponseDto updateDishOptionGroup(Long id, DishOptionGroupRequestDto request);
    void deleteDishOptionGroup(Long id);
}
