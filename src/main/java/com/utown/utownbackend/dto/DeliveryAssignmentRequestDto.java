package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotNull;

public record DeliveryAssignmentRequestDto(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Rider ID is required")
        Long riderId
) {}
