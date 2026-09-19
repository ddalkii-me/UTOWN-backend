package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishOptionStatus;

import java.math.BigDecimal;

public record MenuOptionDto(
        Long id,
        String name,
        BigDecimal additionalPrice,
        DishOptionStatus status,
        Integer sortOrder
) {}
