package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.PositiveOrZero;

public record DishOptionGroupRequestDto(
        @NotNull Long dishId,
        @NotBlank String name,
        @NotNull Boolean required,
        @PositiveOrZero Integer minSelections,
        @PositiveOrZero Integer maxSelections,
        Integer sortOrder
) {
}
