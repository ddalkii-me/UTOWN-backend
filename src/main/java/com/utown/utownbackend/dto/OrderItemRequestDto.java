package com.utown.utownbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderItemRequestDto(
        @NotNull Long dishId,
        @NotNull @Min(1) Integer quantity,
        List<Long> optionIds
) {
}
