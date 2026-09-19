package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationRequestDto(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotNull(message = "Notification type is required")
        NotificationType type,

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Message is required")
        @Size(max = 255, message = "Message must not exceed 255 characters")
        String message
) {}
