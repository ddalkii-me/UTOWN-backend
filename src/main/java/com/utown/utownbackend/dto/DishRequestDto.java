package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DishRequestDto(
        @NotNull Long restaurantId,
        @NotNull Long categoryId,
        @NotBlank String name,
        @NotNull BigDecimal price,
        String description,
        String imageUrl,
        @NotNull DishStatus status,
        Integer sortOrder
) {}
