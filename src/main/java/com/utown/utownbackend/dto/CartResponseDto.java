package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.CartStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponseDto(
        Long id,
        Long userId,
        Long restaurantId,
        String restaurantName,
        CartStatus status,
        BigDecimal totalAmount,
        Integer totalItems,
        List<CartItemResponseDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CartResponseDto empty(Long userId) {
        return new CartResponseDto(
                null,
                userId,
                null,
                null,
                CartStatus.ACTIVE,
                BigDecimal.ZERO,
                0,
                List.of(),
                null,
                null
        );
    }
}
