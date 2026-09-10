package com.utown.utownbackend.dto;

public record AuthResponseDto(
        String token,
        String refreshToken
) {}
