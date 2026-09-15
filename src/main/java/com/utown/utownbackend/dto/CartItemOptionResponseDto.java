package com.utown.utownbackend.dto;

import java.math.BigDecimal;

public record CartItemOptionResponseDto(
        Long id,
        Long dishOptionId,
        String optionName,
        BigDecimal optionPrice
) {}
