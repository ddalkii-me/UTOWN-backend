package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishOptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishOptionRequestDto(
        @NotBlank String name,
        @NotNull Long optionGroupId,
        @NotNull BigDecimal additionalPrice,
        Integer sortOrder,
        @NotNull DishOptionStatus status
) {
}
