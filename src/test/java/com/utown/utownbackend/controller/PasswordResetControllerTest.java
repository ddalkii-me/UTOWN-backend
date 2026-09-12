package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.config.GlobalExceptionHandler;
import com.utown.utownbackend.dto.PasswordResetConfirmDto;
import com.utown.utownbackend.dto.PasswordResetRequestDto;
import com.utown.utownbackend.dto.PasswordResetRequestResponseDto;
import com.utown.utownbackend.dto.PasswordResetVerifyDto;
import com.utown.utownbackend.dto.PasswordResetVerifyResponseDto;
import com.utown.utownbackend.exception.InvalidTokenException;
import com.utown.utownbackend.service.PasswordResetService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Nested
    @DisplayName("POST /api/auth/password-reset/request")
    class RequestPasswordResetEndpointTests {

        @Test
        @DisplayName("Valid phone - returns 200 OK with cooldown and message")
        void request_success() throws Exception {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");
            PasswordResetRequestResponseDto response = new PasswordResetRequestResponseDto("Verification code sent successfully", 60);

            when(passwordResetService.requestPasswordReset(any())).thenReturn(response);

            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Verification code sent successfully"))
                    .andExpect(jsonPath("$.cooldownSeconds").value(60));
        }

        @Test
        @DisplayName("Blank phone - returns 400 Bad Request")
        void request_blankPhone_returnsBadRequest() throws Exception {
            PasswordResetRequestDto request = new PasswordResetRequestDto("");

            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passwordResetService, never()).requestPasswordReset(any());
        }

        @Test
        @DisplayName("Invalid phone format - returns 400 Bad Request")
        void request_invalidPhone_returnsBadRequest() throws Exception {
            PasswordResetRequestDto request = new PasswordResetRequestDto("invalid-phone");

            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passwordResetService, never()).requestPasswordReset(any());
        }

        @Test
        @DisplayName("Cooldown active - returns 400 Bad Request")
        void request_cooldownActive_returnsBadRequest() throws Exception {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            when(passwordResetService.requestPasswordReset(any()))
                    .thenThrow(new IllegalStateException("A reset code was already sent recently. Please wait for the cooldown to expire."));

            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("cooldown")));
        }

        @Test
        @DisplayName("User not found - returns 404 Not Found")
        void request_userNotFound_returnsNotFound() throws Exception {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            when(passwordResetService.requestPasswordReset(any()))
                    .thenThrow(new EntityNotFoundException("User not found with phone: 010-1234-5678"));

            mockMvc.perform(post("/api/auth/password-reset/request")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/verify")
    class VerifyPasswordResetEndpointTests {

        @Test
        @DisplayName("Valid code - returns 200 OK with reset token")
        void verify_success() throws Exception {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");
            PasswordResetVerifyResponseDto response = new PasswordResetVerifyResponseDto("sample.jwt.token", 900);

            when(passwordResetService.verifyPasswordReset(any())).thenReturn(response);

            mockMvc.perform(post("/api/auth/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resetToken").value("sample.jwt.token"))
                    .andExpect(jsonPath("$.expiresInSeconds").value(900));
        }

        @Test
        @DisplayName("Code is not 6 digits - returns 400 Bad Request")
        void verify_non6DigitCode_returnsBadRequest() throws Exception {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "1234");

            mockMvc.perform(post("/api/auth/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passwordResetService, never()).verifyPasswordReset(any());
        }

        @Test
        @DisplayName("Invalid code - returns 400 Bad Request")
        void verify_invalidCode_returnsBadRequest() throws Exception {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            when(passwordResetService.verifyPasswordReset(any()))
                    .thenThrow(new IllegalArgumentException("Invalid verification code"));

            mockMvc.perform(post("/api/auth/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Invalid verification code"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/password-reset/confirm")
    class ConfirmPasswordResetEndpointTests {

        @Test
        @DisplayName("Valid request - returns 200 OK with success message")
        void confirm_success() throws Exception {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("valid.token", "newValidPassword123");

            doNothing().when(passwordResetService).confirmPasswordReset(any());

            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"));

            verify(passwordResetService).confirmPasswordReset(any());
        }

        @Test
        @DisplayName("Password too short (< 8 chars) - returns 400 Bad Request")
        void confirm_shortPassword_returnsBadRequest() throws Exception {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("valid.token", "short");

            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(passwordResetService, never()).confirmPasswordReset(any());
        }

        @Test
        @DisplayName("Invalid or expired token - returns 401 Unauthorized")
        void confirm_invalidToken_returnsUnauthorized() throws Exception {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("invalid.token", "newValidPassword123");

            doThrow(new InvalidTokenException("Invalid or expired password reset token"))
                    .when(passwordResetService).confirmPasswordReset(any());

            mockMvc.perform(post("/api/auth/password-reset/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }
    }
}
