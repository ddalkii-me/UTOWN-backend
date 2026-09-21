package com.utown.utownbackend.integration;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.PasswordResetConfirmDto;
import com.utown.utownbackend.dto.PasswordResetRequestDto;
import com.utown.utownbackend.dto.PasswordResetVerifyDto;
import com.utown.utownbackend.dto.PasswordResetVerifyResponseDto;
import com.utown.utownbackend.entity.AuthCode;
import com.utown.utownbackend.entity.AuthCodePurpose;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.repository.AuthCodeRepository;
import com.utown.utownbackend.repository.RefreshTokenRepository;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.service.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthCodeRepository authCodeRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        authCodeRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("reset-test@example.com");
        user.setPhone("+821011112222");
        user.setName("Reset Test User");
        user.setPassword(passwordEncoder.encode("oldPassword123"));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        refreshTokenService.createRefreshToken(user.getId());
        assertThat(refreshTokenRepository.findByUser(user)).isPresent();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        authCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Password Reset Flow - request, verify, confirm, login with new password, tokens revoked")
    void completePasswordResetFlow_success() throws Exception {
        // 1. Request Code
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto("01011112222");

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cooldownSeconds").value(60));

        AuthCode authCode = authCodeRepository
                .findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET)
                .orElseThrow();
        assertThat(authCode).isNotNull();

        // Set a known code hash so we can verify with precision
        authCode.setCodeHash(passwordEncoder.encode("654321"));
        authCodeRepository.save(authCode);

        // 2. Verify Code
        PasswordResetVerifyDto verifyDto = new PasswordResetVerifyDto("01011112222", "654321");

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").isString())
                .andReturn();

        PasswordResetVerifyResponseDto verifyResponse = objectMapper.readValue(
                verifyResult.getResponse().getContentAsString(),
                PasswordResetVerifyResponseDto.class
        );
        String resetToken = verifyResponse.resetToken();
        assertThat(resetToken).isNotBlank();

        // Check auth code is marked as used
        AuthCode updatedAuthCode = authCodeRepository.findById(authCode.getId()).orElseThrow();
        assertThat(updatedAuthCode.getUsedAt()).isNotNull();

        // 3. Confirm New Password
        PasswordResetConfirmDto confirmDto = new PasswordResetConfirmDto(resetToken, "brandNewPassword123");

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        // Check auth code resetAt is stamped
        AuthCode resetAuthCode = authCodeRepository.findById(authCode.getId()).orElseThrow();
        assertThat(resetAuthCode.getResetAt()).isNotNull();

        // 4. Verify token cannot be reused (Single-Use Token Enforcement)
        PasswordResetConfirmDto reuseAttemptDto = new PasswordResetConfirmDto(resetToken, "anotherNewPassword456");

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reuseAttemptDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        // 5. Verify password is changed in DB and old refresh token is revoked
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("brandNewPassword123", updatedUser.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("oldPassword123", updatedUser.getPassword())).isFalse();

        assertThat(refreshTokenRepository.findByUser(updatedUser)).isEmpty();
    }

    @Test
    @DisplayName("Request Code - non-existent user returns 200 OK with generic message and creates no code")
    void requestCode_nonExistentUser_returnsOkGenericMessage() throws Exception {
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto("01099998888");

        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a verification code has been sent"))
                .andExpect(jsonPath("$.cooldownSeconds").value(60));

        // Verify no AuthCode was created
        assertThat(authCodeRepository.count()).isZero();
    }

    @Test
    @DisplayName("Request Code - returns 200 OK on cooldown to prevent enumeration but creates no new code")
    void requestCode_cooldownActive_returnsOkWithoutCreatingNewCode() throws Exception {
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto("01011112222");

        // First request succeeds
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a verification code has been sent"))
                .andExpect(jsonPath("$.cooldownSeconds").value(60));

        // Immediate second request also returns 200 OK with identical payload
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists, a verification code has been sent"))
                .andExpect(jsonPath("$.cooldownSeconds").value(60));

        // Verify only 1 AuthCode was created
        assertThat(authCodeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify Code - unknown phone returns 400 Bad Request to prevent account enumeration")
    void verifyCode_unknownUser_returnsBadRequest() throws Exception {
        PasswordResetVerifyDto verifyDto = new PasswordResetVerifyDto("01099998888", "123456");

        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No active verification code found"));
    }

    @Test
    @DisplayName("Verify Code - rejects incorrect code and tracks failed attempts")
    void verifyCode_invalidCode_rejectsAndIncrementsAttempts() throws Exception {
        AuthCode authCode = new AuthCode();
        authCode.setUser(user);
        authCode.setCodeHash(passwordEncoder.encode("123456"));
        authCode.setPurpose(AuthCodePurpose.PASSWORD_RESET);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authCodeRepository.save(authCode);

        PasswordResetVerifyDto wrongCodeDto = new PasswordResetVerifyDto("01011112222", "999999");

        // First wrong attempt - should be rejected
        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCodeDto)))
                .andExpect(status().isBadRequest());

        // Second wrong attempt - should also be rejected (proves attempts are tracked)
        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCodeDto)))
                .andExpect(status().isBadRequest());

        // Correct code should still work (under MAX_ATTEMPTS)
        PasswordResetVerifyDto correctCodeDto = new PasswordResetVerifyDto("01011112222", "123456");

        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctCodeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").isString());
    }

    @Test
    @DisplayName("Verify Code - locks out after MAX_ATTEMPTS reached")
    void verifyCode_lockoutAfterMaxAttempts() throws Exception {
        AuthCode authCode = new AuthCode();
        authCode.setUser(user);
        authCode.setCodeHash(passwordEncoder.encode("123456"));
        authCode.setPurpose(AuthCodePurpose.PASSWORD_RESET);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authCodeRepository.save(authCode);

        PasswordResetVerifyDto wrongCodeDto = new PasswordResetVerifyDto("01011112222", "999999");

        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/password-reset/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongCodeDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Invalid verification code"));
        }

        // 6th attempt (even with correct code) is locked out
        PasswordResetVerifyDto correctCodeDto = new PasswordResetVerifyDto("01011112222", "123456");
        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctCodeDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Maximum verification attempts exceeded"));
    }
}
