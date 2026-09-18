package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishStatus;

import java.math.BigDecimal;
import java.util.List;

public record MenuDishDto(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        DishStatus status,
        Integer sortOrder,
        List<MenuOptionGroupDto> optionGroups
) {}
