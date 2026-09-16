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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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

    private User user;

    @BeforeEach
    void setUp() {
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
        assertThat(authCodeRepository.findAll()).noneMatch(ac -> "+821099998888".equals(ac.getUser().getPhone()));
    }

    @Test
    @DisplayName("Request Code - rejects when cooldown is active")
    void requestCode_cooldownActive_rejects() throws Exception {
        PasswordResetRequestDto requestDto = new PasswordResetRequestDto("01011112222");

        // First request succeeds
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        // Immediate second request fails with 400 Bad Request
        mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Verify Code - rejects incorrect code and increments attempts")
    void verifyCode_invalidCode_rejectsAndIncrementsAttempts() throws Exception {
        AuthCode authCode = new AuthCode();
        authCode.setUser(user);
        authCode.setCodeHash(passwordEncoder.encode("123456"));
        authCode.setPurpose(AuthCodePurpose.PASSWORD_RESET);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authCodeRepository.save(authCode);

        PasswordResetVerifyDto wrongCodeDto = new PasswordResetVerifyDto("01011112222", "999999");

        mockMvc.perform(post("/api/auth/password-reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCodeDto)))
                .andExpect(status().isBadRequest());

        AuthCode reloaded = authCodeRepository.findById(authCode.getId()).orElseThrow();
        assertThat(reloaded.getAttempts()).isEqualTo(1);
        assertThat(reloaded.getUsedAt()).isNull();
    }
}
