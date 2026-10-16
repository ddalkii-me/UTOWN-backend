package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for user login")
public record LoginRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?(?:[\\-\\s()]*[0-9]){7,}[\\-\\s()0-9]*$", message = "Invalid phone number format")
        @Schema(description = "User's registered phone number", example = "01012345678")
        String phone,

        @NotBlank(message = "Password is required")
        @Schema(description = "User's password", example = "password123")
        String password
) {}
