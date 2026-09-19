package com.utown.utownbackend.dto;

import java.util.List;

public record MenuCategoryDto(
        Long id,
        String name,
        String description,
        String imageUrl,
        Integer priority,
        List<MenuDishDto> dishes
) {}
