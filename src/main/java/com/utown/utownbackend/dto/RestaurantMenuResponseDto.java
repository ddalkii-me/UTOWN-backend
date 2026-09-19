package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RestaurantStatus;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantMenuResponseDto(
        Long restaurantId,
        String restaurantName,
        RestaurantStatus restaurantStatus,
        BigDecimal minimumOrderAmount,
        List<MenuCategoryDto> categories
) {}
