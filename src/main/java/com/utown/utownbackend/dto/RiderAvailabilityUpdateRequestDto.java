package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotNull;

public record RiderAvailabilityUpdateRequestDto(
        @NotNull(message = "Availability is required")
        Boolean availability
) {}
