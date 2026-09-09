package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9\\-\\s()]{8,20}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "Password is required")
        String password
) {}
