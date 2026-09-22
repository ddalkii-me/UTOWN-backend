package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.debug("Entering register method");
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.debug("Entering login method");
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(@Valid @RequestBody com.utown.utownbackend.dto.TokenRefreshRequestDto request) {
        log.debug("Entering refreshToken method");
        AuthResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(java.security.Principal principal) {
        log.debug("Entering logout method with principal={}", principal);
        if (principal != null) {
            authService.logout(principal.getName());
        }
        return ResponseEntity.noContent().build();
    }
}
