package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RestaurantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RestaurantRequestDto(
        @NotNull Long ownerId,
        @NotNull Long typeId,
        @NotNull Long cityId,
        @NotBlank String name,
        String description,
        String address,
        String phone,
        String logoUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal minimumOrderAmount,
        @NotNull RestaurantStatus status
) {}
