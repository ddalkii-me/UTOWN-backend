package com.utown.utownbackend.dto;

public record RatingResponseDto(
        Long id,
        Long userId,
        Long restaurantId,
        Long orderId,
        Integer score,
        String comment
) {
}