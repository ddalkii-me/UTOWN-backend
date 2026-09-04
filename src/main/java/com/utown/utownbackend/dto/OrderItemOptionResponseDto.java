package com.utown.utownbackend.dto;

import java.math.BigDecimal;

public record OrderItemOptionResponseDto(
        Long id,
        String optionName,
        BigDecimal optionPrice
) {
}
