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
        RestaurantStatus status,
        boolean isOpenNow,
        BigDecimal averageRating,
        Integer deliveryTimeMinutes
) {
    public RestaurantResponseDto(
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
            RestaurantStatus status,
            boolean isOpenNow
    ) {
        this(id, ownerId, typeId, cityId, name, description, address, phone, logoUrl,
                latitude, longitude, minimumOrderAmount, status, isOpenNow, null, null);
    }
}
