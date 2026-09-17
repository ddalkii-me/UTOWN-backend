package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import com.utown.utownbackend.entity.TransportType;

import java.time.LocalDateTime;

public record DeliveryAssignmentResponseDto(
        Long id,
        Long orderId,
        String orderNumber,
        Long restaurantId,
        String restaurantName,
        Long riderId,
        String riderName,
        String riderPhone,
        TransportType transportType,
        DeliveryAssignmentStatus status,
        LocalDateTime assignedAt,
        LocalDateTime acceptedAt,
        LocalDateTime pickedUpAt,
        LocalDateTime deliveredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
