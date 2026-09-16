package com.utown.utownbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddToCartRequestDto(
        @NotNull(message = "Restaurant ID is required")
        Long restaurantId,

        @NotNull(message = "Dish ID is required")
        Long dishId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        List<Long> optionIds
) {}
