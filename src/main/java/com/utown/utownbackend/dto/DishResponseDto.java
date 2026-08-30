package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishResponseDto(
        Long id,
        Long restaurantId,
        Long categoryId,
        String name,
        BigDecimal price,
        String description,
        String imageUrl,
        DishStatus status,
        Integer sortOrder,
        LocalDateTime deletedAt
) {}
