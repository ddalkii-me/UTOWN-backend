package com.utown.utownbackend.dto;

import java.math.BigDecimal;

public record AddressResponseDto(
        Long id,
        Long userId,
        Long cityId,
        Long deliveryAreaId,
        String label,
        String recipientName,
        String phone,
        String addressLine,
        String postalCode,
        BigDecimal latitude,
        BigDecimal longitude
) {}
