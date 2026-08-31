package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DishOptionGroupRequestDto(
        @NotNull Long dishId,
        @NotBlank String name,
        @NotNull Boolean required,
        Integer minSelections,
        Integer maxSelections,
        Integer sortOrder
) {
}
