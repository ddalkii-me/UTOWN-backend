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
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthCodeRepository authCodeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(1L);
        user.setPhone("+821012345678");
    }

    @Nested
    @DisplayName("requestPasswordReset")
    class RequestPasswordResetTests {

        @Test
        @DisplayName("Success - creates new AuthCode with 60s cooldown, 5m expiry, dispatches SMS")
        void requestPasswordReset_success() {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded6DigitCode");

            PasswordResetRequestResponseDto response = passwordResetService.requestPasswordReset(request);

            assertThat(response).isNotNull();
            assertThat(response.cooldownSeconds()).isEqualTo(60);
            assertThat(response.message()).isEqualTo("If an account exists, a verification code has been sent");

            ArgumentCaptor<AuthCode> authCodeCaptor = ArgumentCaptor.forClass(AuthCode.class);
            verify(authCodeRepository).save(authCodeCaptor.capture());

            AuthCode saved = authCodeCaptor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getCodeHash()).isEqualTo("encoded6DigitCode");
            assertThat(saved.getPurpose()).isEqualTo(AuthCodePurpose.PASSWORD_RESET);
            assertThat(saved.getAttempts()).isEqualTo(0);
            assertThat(saved.getUsedAt()).isNull();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));

            verify(smsService).sendVerificationCode(eq("+821012345678"), anyString());
        }

        @Test
        @DisplayName("User not found - returns generic response and does not save AuthCode or send SMS")
        void requestPasswordReset_userNotFound() {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.empty());

            PasswordResetRequestResponseDto response = passwordResetService.requestPasswordReset(request);

            assertThat(response).isNotNull();
            assertThat(response.cooldownSeconds()).isEqualTo(60);
            assertThat(response.message()).isEqualTo("If an account exists, a verification code has been sent");

            verify(authCodeRepository, never()).save(any());
            verify(smsService, never()).sendVerificationCode(any(), any());
        }

        @Test
        @DisplayName("Invalid phone format - throws IllegalArgumentException")
        void requestPasswordReset_invalidPhone() {
            PasswordResetRequestDto request = new PasswordResetRequestDto("invalid-phone");

            assertThatThrownBy(() -> passwordResetService.requestPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(userRepository, never()).findByPhoneAndDeletedAtIsNull(any());
        }

        @Test
        @DisplayName("Cooldown active (< 60s since last request) - returns generic 200 response to prevent enumeration")
        void requestPasswordReset_cooldownActive() {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            AuthCode recentCode = new AuthCode();
            recentCode.setCreatedAt(LocalDateTime.now().minusSeconds(20));

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(recentCode));

            PasswordResetRequestResponseDto response = passwordResetService.requestPasswordReset(request);

            assertThat(response).isNotNull();
            assertThat(response.message()).isEqualTo("If an account exists, a verification code has been sent");
            assertThat(response.cooldownSeconds()).isEqualTo(60);

            verify(authCodeRepository, never()).save(any());
            verify(smsService, never()).sendVerificationCode(anyString(), anyString());
        }

        @Test
        @DisplayName("Cooldown elapsed (>= 60s since last request) - succeeds and creates code")
        void requestPasswordReset_cooldownElapsed() {
            PasswordResetRequestDto request = new PasswordResetRequestDto("010-1234-5678");

            AuthCode olderCode = new AuthCode();
            olderCode.setCreatedAt(LocalDateTime.now().minusSeconds(65));

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(olderCode));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedNewCode");

            PasswordResetRequestResponseDto response = passwordResetService.requestPasswordReset(request);

            assertThat(response).isNotNull();
            verify(authCodeRepository).save(any(AuthCode.class));
        }
    }

    @Nested
    @DisplayName("verifyPasswordReset")
    class VerifyPasswordResetTests {

        @Test
        @DisplayName("Success - marks AuthCode as used, returns 15m reset token")
        void verifyPasswordReset_success() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setExpiresAt(LocalDateTime.now().plusMinutes(4));
            authCode.setAttempts(0);

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(authCode));
            when(authCodeRepository.incrementAttemptsIfUnderLimit(10L, 5)).thenReturn(1);
            when(passwordEncoder.matches("123456", "hashedCode")).thenReturn(true);
            when(jwtUtil.generatePasswordResetToken("+821012345678", 1L, 10L)).thenReturn("mocked.reset.token");

            PasswordResetVerifyResponseDto response = passwordResetService.verifyPasswordReset(request);

            assertThat(response).isNotNull();
            assertThat(response.resetToken()).isEqualTo("mocked.reset.token");
            assertThat(response.expiresInSeconds()).isEqualTo(900);

            verify(authCodeRepository).incrementAttemptsIfUnderLimit(10L, 5);
            assertThat(authCode.getUsedAt()).isNotNull();
            verify(authCodeRepository).save(authCode);
        }

        @Test
        @DisplayName("User not found - throws IllegalArgumentException to prevent enumeration")
        void verifyPasswordReset_userNotFound() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No active verification code found");
        }

        @Test
        @DisplayName("No active unused code found - throws IllegalArgumentException")
        void verifyPasswordReset_noActiveCode() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No active verification code");
        }

        @Test
        @DisplayName("Code expired - throws IllegalArgumentException")
        void verifyPasswordReset_expiredCode() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setExpiresAt(LocalDateTime.now().minusSeconds(10)); // expired

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(authCode));

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Max attempts exceeded (>= 5) - throws IllegalArgumentException")
        void verifyPasswordReset_maxAttemptsExceeded() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setAttempts(5);

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(authCode));

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum verification attempts exceeded");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Atomic attempt limit reached - throws IllegalArgumentException without evaluating encoder")
        void verifyPasswordReset_atomicLimitReached_throwsIllegalArgumentException() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "123456");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setAttempts(4);

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(authCode));
            when(authCodeRepository.incrementAttemptsIfUnderLimit(10L, 5)).thenReturn(0);

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum verification attempts exceeded");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Invalid code - increments attempts under limit and throws IllegalArgumentException")
        void verifyPasswordReset_invalidCode_incrementsAttempts() {
            PasswordResetVerifyDto request = new PasswordResetVerifyDto("010-1234-5678", "999999");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setAttempts(2);

            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET))
                    .thenReturn(Optional.of(authCode));
            when(authCodeRepository.incrementAttemptsIfUnderLimit(10L, 5)).thenReturn(1);
            when(passwordEncoder.matches("999999", "hashedCode")).thenReturn(false);

            assertThatThrownBy(() -> passwordResetService.verifyPasswordReset(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid verification code");

            verify(authCodeRepository).incrementAttemptsIfUnderLimit(10L, 5);
            assertThat(authCode.getUsedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("confirmPasswordReset")
    class ConfirmPasswordResetTests {

        @Test
        @DisplayName("Success - updates password, marks reset token used, and revokes all refresh tokens")
        void confirmPasswordReset_success() {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("valid.reset.token", "newSecurePassword123");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setUsedAt(LocalDateTime.now().minusMinutes(1));

            when(jwtUtil.validatePasswordResetToken("valid.reset.token")).thenReturn(true);
            when(jwtUtil.extractPasswordResetPhone("valid.reset.token")).thenReturn("+821012345678");
            when(jwtUtil.extractPasswordResetCodeId("valid.reset.token")).thenReturn(10L);
            when(authCodeRepository.findById(10L)).thenReturn(Optional.of(authCode));
            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newSecurePassword123")).thenReturn("newHashedPassword");

            passwordResetService.confirmPasswordReset(request);

            assertThat(user.getPassword()).isEqualTo("newHashedPassword");
            assertThat(authCode.getResetAt()).isNotNull();
            verify(authCodeRepository).save(authCode);
            verify(userRepository).save(user);
            verify(refreshTokenService).deleteByUserId(1L);
        }

        @Test
        @DisplayName("Reset token already used - throws InvalidTokenException to prevent reuse")
        void confirmPasswordReset_tokenAlreadyUsed_throwsInvalidTokenException() {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("already.used.token", "newSecurePassword123");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setUsedAt(LocalDateTime.now().minusMinutes(5));
            authCode.setResetAt(LocalDateTime.now().minusMinutes(1)); // already used

            when(jwtUtil.validatePasswordResetToken("already.used.token")).thenReturn(true);
            when(jwtUtil.extractPasswordResetPhone("already.used.token")).thenReturn("+821012345678");
            when(jwtUtil.extractPasswordResetCodeId("already.used.token")).thenReturn(10L);
            when(authCodeRepository.findById(10L)).thenReturn(Optional.of(authCode));

            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("already been used");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("Missing codeId claim in token - throws InvalidTokenException")
        void confirmPasswordReset_missingCodeId_throwsInvalidTokenException() {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("token.missing.codeId", "newSecurePassword123");

            when(jwtUtil.validatePasswordResetToken("token.missing.codeId")).thenReturn(true);
            when(jwtUtil.extractPasswordResetPhone("token.missing.codeId")).thenReturn("+821012345678");
            when(jwtUtil.extractPasswordResetCodeId("token.missing.codeId")).thenReturn(null);

            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("claims");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("Invalid or expired reset token - throws InvalidTokenException")
        void confirmPasswordReset_invalidToken() {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("invalid.or.expired.token", "newSecurePassword123");

            when(jwtUtil.validatePasswordResetToken("invalid.or.expired.token")).thenReturn(false);

            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("User not found from token - throws EntityNotFoundException")
        void confirmPasswordReset_userNotFound() {
            PasswordResetConfirmDto request = new PasswordResetConfirmDto("valid.token.for.deleted.user", "newSecurePassword123");

            AuthCode authCode = TestDataFactory.createAuthCode(10L, user, "hashedCode", AuthCodePurpose.PASSWORD_RESET);
            authCode.setUsedAt(LocalDateTime.now().minusMinutes(1));

            when(jwtUtil.validatePasswordResetToken("valid.token.for.deleted.user")).thenReturn(true);
            when(jwtUtil.extractPasswordResetPhone("valid.token.for.deleted.user")).thenReturn("+821012345678");
            when(jwtUtil.extractPasswordResetCodeId("valid.token.for.deleted.user")).thenReturn(10L);
            when(authCodeRepository.findById(10L)).thenReturn(Optional.of(authCode));
            when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.confirmPasswordReset(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUserId(any());
        }
    }
}
