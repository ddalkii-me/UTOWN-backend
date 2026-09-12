package com.utown.utownbackend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 9, message = "Password must be at least 9 characters")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword
) {
        @AssertTrue(message = "New passwords do not match")
        public boolean isPasswordsMatch() {
                if (newPassword == null || confirmNewPassword == null) {
                        return true;
                }
                return newPassword.equals(confirmNewPassword);
        }
}
