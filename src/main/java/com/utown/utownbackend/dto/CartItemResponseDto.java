package com.utown.utownbackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemResponseDto(
        Long id,
        Long dishId,
        String dishName,
        String dishImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        List<CartItemOptionResponseDto> options
) {}
