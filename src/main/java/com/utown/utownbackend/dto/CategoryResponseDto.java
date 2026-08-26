package com.utown.utownbackend.dto;

public record CategoryResponseDto(
        Long id,
        Long restaurantId,
        String name,
        String description
) {}