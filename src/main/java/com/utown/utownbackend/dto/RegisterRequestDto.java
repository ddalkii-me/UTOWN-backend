package com.utown.utownbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9\\-\\s()]{8,20}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 9, message = "Password must be at least 9 characters")
        String password,

        @NotBlank(message = "Confirm password is required")
        String confirmPassword,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification token is required")
        String verificationToken
) {
        @jakarta.validation.constraints.AssertTrue(message = "Passwords do not match")
        public boolean isPasswordsMatch() {
                if (password == null || confirmPassword == null) {
                        return true; // Let @NotBlank handle nulls
                }
                return password.equals(confirmPassword);
        }
}
