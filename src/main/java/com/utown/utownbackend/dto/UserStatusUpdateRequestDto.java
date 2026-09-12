package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequestDto(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
