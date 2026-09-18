package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;

import java.time.LocalDateTime;

public record UserProfileResponseDto(
        Long id,
        String phone,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime emailVerifiedAt,
        LocalDateTime phoneVerifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
