package com.utown.utownbackend.dto;

public record CategoryRequestDto(
        Long restaurantId,
        String name,
        String description
) {}