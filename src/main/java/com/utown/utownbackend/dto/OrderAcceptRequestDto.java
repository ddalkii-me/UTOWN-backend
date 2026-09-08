package com.utown.utownbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderAcceptRequestDto(
        @NotNull(message = "Estimated cooking minutes is required")
        @Min(value = 1, message = "Estimated cooking minutes must be at least 1")
        Integer estimatedCookingMinutes
) {
}
