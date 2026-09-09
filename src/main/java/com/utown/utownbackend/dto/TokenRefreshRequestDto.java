package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequestDto(
        @NotBlank String refreshToken
) {}
