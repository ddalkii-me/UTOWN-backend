package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.entity.PaymentMethod;
import com.utown.utownbackend.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        Long userId,
        Long restaurantId,
        Long addressId,
        String orderNumber,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String deliveryNote,
        Integer estimatedCookingMinutes,
        String rejectionReason,
        BigDecimal subTotal,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime acceptedAt,
        LocalDateTime rejectedAt,
        LocalDateTime readyAt,
        LocalDateTime pickedUpAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        List<OrderItemResponseDto> items
) {
}
