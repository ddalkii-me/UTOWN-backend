package com.utown.utownbackend.dto;

public record PasswordResetVerifyResponseDto(
        String resetToken,
        long expiresInSeconds
) {}
