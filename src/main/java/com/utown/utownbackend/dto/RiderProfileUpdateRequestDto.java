package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.TransportType;
import jakarta.validation.constraints.NotNull;

public record RiderProfileUpdateRequestDto(
        @NotNull(message = "Transport type is required")
        TransportType transportType
) {}
