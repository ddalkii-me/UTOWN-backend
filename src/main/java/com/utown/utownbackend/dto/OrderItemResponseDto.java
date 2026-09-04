package com.utown.utownbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemResponseDto(
        Long id,
        Long dishId,
        String dishName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subTotal,
        List<OrderItemOptionResponseDto> options
) {
}
