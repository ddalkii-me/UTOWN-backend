package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RiderStatus;
import jakarta.validation.constraints.NotNull;

public record RiderStatusUpdateRequestDto(
        @NotNull(message = "Status is required")
        RiderStatus status
) {}
