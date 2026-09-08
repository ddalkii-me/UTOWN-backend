package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequestDto(
        @NotNull Long userId,
        @NotNull Long restaurantId,
        @NotNull Long addressId,
        String deliveryNote,
        @NotNull PaymentMethod paymentMethod,
        @NotEmpty @Valid List<OrderItemRequestDto> items
        ) {
}
