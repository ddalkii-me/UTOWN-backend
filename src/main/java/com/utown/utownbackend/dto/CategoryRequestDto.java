package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequestDto(
        @NotNull Long restaurantId,
        @NotBlank String name,
        String description
) {}