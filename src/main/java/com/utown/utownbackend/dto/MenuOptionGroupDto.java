package com.utown.utownbackend.dto;

import java.util.List;

public record MenuOptionGroupDto(
        Long id,
        String name,
        Boolean required,
        Integer minSelections,
        Integer maxSelections,
        Integer sortOrder,
        List<MenuOptionDto> options
) {}
