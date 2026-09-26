package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?(?:[\\-\\s()]*[0-9]){7,}[\\-\\s()0-9]*$", message = "Invalid phone number format")
        String phone
) {}
