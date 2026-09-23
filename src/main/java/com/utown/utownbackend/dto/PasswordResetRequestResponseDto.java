package com.utown.utownbackend.dto;

public record PasswordResetRequestResponseDto(
        String message,
        long cooldownSeconds
) {}
