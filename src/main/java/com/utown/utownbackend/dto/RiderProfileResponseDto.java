package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.TransportType;

import java.time.LocalDateTime;

public record RiderProfileResponseDto(
        Long id,
        Long userId,
        String userName,
        String userPhone,
        String userEmail,
        TransportType transportType,
        Boolean availability,
        RiderStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
