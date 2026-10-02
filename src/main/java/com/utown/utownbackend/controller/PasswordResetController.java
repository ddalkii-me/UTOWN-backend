package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.PasswordResetConfirmDto;
import com.utown.utownbackend.dto.PasswordResetRequestDto;
import com.utown.utownbackend.dto.PasswordResetRequestResponseDto;
import com.utown.utownbackend.dto.PasswordResetVerifyDto;
import com.utown.utownbackend.dto.PasswordResetVerifyResponseDto;
import com.utown.utownbackend.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<PasswordResetRequestResponseDto> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequestDto request) {
        log.debug("Entering requestPasswordReset method");
        PasswordResetRequestResponseDto response = passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PasswordResetVerifyResponseDto> verifyPasswordReset(
            @Valid @RequestBody PasswordResetVerifyDto request) {
        log.debug("Entering verifyPasswordReset method");
        PasswordResetVerifyResponseDto response = passwordResetService.verifyPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmDto request) {
        log.debug("Entering confirmPasswordReset method");
        passwordResetService.confirmPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
