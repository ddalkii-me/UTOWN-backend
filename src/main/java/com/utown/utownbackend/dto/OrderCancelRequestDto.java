package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderCancelRequestDto(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {}
