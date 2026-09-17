package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.PasswordResetConfirmDto;
import com.utown.utownbackend.dto.PasswordResetRequestDto;
import com.utown.utownbackend.dto.PasswordResetRequestResponseDto;
import com.utown.utownbackend.dto.PasswordResetVerifyDto;
import com.utown.utownbackend.dto.PasswordResetVerifyResponseDto;
import com.utown.utownbackend.entity.AuthCode;
import com.utown.utownbackend.entity.AuthCodePurpose;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.exception.InvalidTokenException;
import com.utown.utownbackend.repository.AuthCodeRepository;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.security.JwtUtil;
import com.utown.utownbackend.util.PhoneUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    public static final long COOLDOWN_SECONDS = 60L;
    public static final long EXPIRATION_MINUTES = 5L;
    public static final int MAX_ATTEMPTS = 5;
    public static final long RESET_TOKEN_EXPIRATION_SECONDS = 900L;

    private final UserRepository userRepository;
    private final AuthCodeRepository authCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SmsService smsService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public PasswordResetRequestResponseDto requestPasswordReset(PasswordResetRequestDto request) {
        String normalizedPhone = PhoneUtil.normalizePhone(request.phone());
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        Optional<User> userOpt = userRepository.findByPhoneAndDeletedAtIsNull(normalizedPhone);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent phone: {}", normalizedPhone);
            return new PasswordResetRequestResponseDto("If an account exists, a verification code has been sent", COOLDOWN_SECONDS);
        }

        User user = userOpt.get();

        Optional<AuthCode> latestCodeOpt = authCodeRepository
                .findTopByUserAndPurposeOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET);

        if (latestCodeOpt.isPresent()) {
            AuthCode latestCode = latestCodeOpt.get();
            if (latestCode.getCreatedAt() != null) {
                long secondsSinceLastRequest = Duration.between(latestCode.getCreatedAt(), LocalDateTime.now()).getSeconds();
                if (secondsSinceLastRequest < COOLDOWN_SECONDS) {
                    long remaining = COOLDOWN_SECONDS - secondsSinceLastRequest;
                    throw new IllegalStateException("A reset code was already sent recently. Please wait for the cooldown to expire (" + remaining + " seconds remaining).");
                }
            }
        }

        int codeInt = 100000 + secureRandom.nextInt(900000);
        String rawCode = String.valueOf(codeInt);
        String codeHash = passwordEncoder.encode(rawCode);

        AuthCode authCode = new AuthCode();
        authCode.setUser(user);
        authCode.setCodeHash(codeHash);
        authCode.setPurpose(AuthCodePurpose.PASSWORD_RESET);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));

        authCodeRepository.save(authCode);

        smsService.sendVerificationCode(normalizedPhone, rawCode);
        log.info("Dispatched password reset verification code to phone: {}", normalizedPhone);

        return new PasswordResetRequestResponseDto("If an account exists, a verification code has been sent", COOLDOWN_SECONDS);
    }

    @Override
    @Transactional
    public PasswordResetVerifyResponseDto verifyPasswordReset(PasswordResetVerifyDto request) {
        String normalizedPhone = PhoneUtil.normalizePhone(request.phone());
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        User user = userRepository.findByPhoneAndDeletedAtIsNull(normalizedPhone)
                .orElseThrow(() -> new EntityNotFoundException("User not found with phone: " + request.phone()));

        AuthCode authCode = authCodeRepository
                .findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code found"));

        if (authCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Maximum verification attempts exceeded");
        }

        if (authCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired");
        }

        if (!passwordEncoder.matches(request.code(), authCode.getCodeHash())) {
            authCodeRepository.incrementAttempts(authCode.getId());
            throw new IllegalArgumentException("Invalid verification code");
        }

        authCode.setUsedAt(LocalDateTime.now());
        authCodeRepository.save(authCode);

        String resetToken = jwtUtil.generatePasswordResetToken(user.getPhone(), user.getId(), authCode.getId());

        return new PasswordResetVerifyResponseDto(resetToken, RESET_TOKEN_EXPIRATION_SECONDS);
    }

    @Override
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmDto request) {
        if (!jwtUtil.validatePasswordResetToken(request.resetToken())) {
            throw new InvalidTokenException("Invalid or expired password reset token");
        }

        String phone = jwtUtil.extractPasswordResetPhone(request.resetToken());
        if (phone == null || phone.isBlank()) {
            throw new InvalidTokenException("Invalid token claims");
        }

        Long codeId = jwtUtil.extractPasswordResetCodeId(request.resetToken());
        if (codeId == null) {
            throw new InvalidTokenException("Invalid token claims");
        }

        AuthCode authCode = authCodeRepository.findById(codeId)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (authCode.getResetAt() != null) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (authCode.getUsedAt() == null) {
            throw new InvalidTokenException("Verification code was not verified");
        }

        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!authCode.getUser().getId().equals(user.getId())) {
            throw new InvalidTokenException("Invalid token claims");
        }

        authCode.setResetAt(LocalDateTime.now());
        authCodeRepository.save(authCode);

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.deleteByUserId(user.getId());

        log.info("Password successfully reset and refresh tokens revoked for user ID: {}", user.getId());
    }
}
