package com.utown.utownbackend.dto;

public record FavoriteRestaurantResponseDto(
        Long id,
        Long userId,
        Long restaurantId
) {}