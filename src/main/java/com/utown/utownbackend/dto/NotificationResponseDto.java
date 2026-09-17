package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        Long userId,
        NotificationType type,
        String title,
        String message,
        Boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
