package com.utown.utownbackend.dto;

public record RestaurantDeliveryAreaResponseDto(
        Long id,
        Long restaurantId,
        Long deliveryAreaId,
        String deliveryAreaName
) {}