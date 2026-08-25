package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RestaurantStatus;

import java.math.BigDecimal;

public record RestaurantResponseDto(
        Long id,
        Long ownerId,
        Long typeId,
        Long cityId,
        String name,
        String description,
        String address,
        String phone,
        String logoUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal minimumOrderAmount,
        RestaurantStatus status
) {}
