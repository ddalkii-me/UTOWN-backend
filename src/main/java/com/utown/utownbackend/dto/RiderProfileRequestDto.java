package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.TransportType;
import jakarta.validation.constraints.NotNull;

public record RiderProfileRequestDto(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Transport type is required")
        TransportType transportType,

        Boolean availability,

        RiderStatus status
) {}
