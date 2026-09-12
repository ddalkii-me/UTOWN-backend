package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequestDto(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Message is required")
        String message
) {}
