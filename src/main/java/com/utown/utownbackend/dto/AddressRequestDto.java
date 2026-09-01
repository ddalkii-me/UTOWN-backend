package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddressRequestDto(

        @NotNull(message = "User is required")
        Long userId,

        @NotNull(message = "City is required")
        Long cityId,

        @NotNull(message = "Delivery area is required")
        Long deliveryAreaId,

        String label,

        @NotBlank(message = "Recipient name is required")
        String recipientName,

        String phone,

        @NotBlank(message = "Address is required")
        String addressLine,

        String postalCode,

        BigDecimal latitude,

        BigDecimal longitude
) {}
