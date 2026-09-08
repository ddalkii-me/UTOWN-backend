package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderDeclineRequestDto(
        @NotBlank(message = "Rejection reason is required")
        String reason
) {
}
