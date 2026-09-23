package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequestDto(
        Long userId,

        @NotNull(message = "Address ID is required")
        Long addressId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String deliveryNote
) {}
