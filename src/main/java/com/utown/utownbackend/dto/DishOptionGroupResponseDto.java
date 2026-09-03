package com.utown.utownbackend.dto;

import java.time.LocalDateTime;

public record DishOptionGroupResponseDto(
        Long id,
        Long dishId,
        String name,
        Boolean required,
        Integer minSelections,
        Integer maxSelections,
        Integer sortOrder,
        LocalDateTime deletedAt
) {
}
