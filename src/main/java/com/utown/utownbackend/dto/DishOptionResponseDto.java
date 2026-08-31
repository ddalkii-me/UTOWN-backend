package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishOptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishOptionResponseDto(
        Long id,
        Long optionGroupId,
        String name,
        BigDecimal additionalPrice,
        Integer sortOrder,
        DishOptionStatus status,
        LocalDateTime deletedAt
) {
}
