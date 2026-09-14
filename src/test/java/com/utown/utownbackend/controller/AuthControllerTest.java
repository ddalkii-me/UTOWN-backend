package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.dto.TokenRefreshRequestDto;
import com.utown.utownbackend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.utown.utownbackend.exception.InvalidTokenException;
import com.utown.utownbackend.entity.RefreshToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for pure controller testing
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("register - success returns 201 Created")
    void register_success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "010-1234-5678",
                "secretPassword",
                "secretPassword",
                "Test Customer",
                "test@example.com",
                "123456"
        );
        AuthResponseDto response = new AuthResponseDto("jwt-token", "refresh-token");

        Mockito.when(authService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }
    @Test
    @DisplayName("register - invalid phone format returns 400 Bad Request")
    void register_invalidPhone_returns400() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "abc", // Invalid phone format
                "secretPassword",
                "secretPassword",
                "Test Customer",
                "test@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("login - success returns 200 OK")
    void login_success() throws Exception {
        LoginRequestDto request = new LoginRequestDto("010-1234-5678", "secretPassword");
        AuthResponseDto response = new AuthResponseDto("jwt-token", "refresh-token");

        Mockito.when(authService.login(any(LoginRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }
    @Test
    @DisplayName("login - invalid phone format returns 400 Bad Request")
    void login_invalidPhone_returns400() throws Exception {
        LoginRequestDto request = new LoginRequestDto("abc", "secretPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("refresh - success returns 200 OK")
    void refresh_success() throws Exception {
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("valid-refresh-token");
        AuthResponseDto response = new AuthResponseDto("new-jwt-token", "new-refresh-token");

        Mockito.when(authService.refreshToken(any(TokenRefreshRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    @DisplayName("refresh - invalid token returns 401 Unauthorized with descriptive detail")
    void refresh_invalidToken_returns401() throws Exception {
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("invalid-token");

        Mockito.when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                .thenThrow(new InvalidTokenException("Refresh token is invalid or missing!"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Refresh token is invalid or missing!"));
    }

    @Test
    @DisplayName("refresh - suspended user returns 403 Forbidden")
    void refresh_suspendedUser_returns403() throws Exception {
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("valid-token");

        Mockito.when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                .thenThrow(new DisabledException("User is not active"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Disabled"))
                .andExpect(jsonPath("$.detail").value("Your account is suspended or inactive."));
    }

    @Test
    @DisplayName("login - locked user returns 403 Forbidden")
    void login_lockedUser_returns403() throws Exception {
        LoginRequestDto request = new LoginRequestDto("010-1234-5678", "secretPassword");

        Mockito.when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new LockedException("User account is locked"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Disabled"))
                .andExpect(jsonPath("$.detail").value("Your account is suspended or inactive."));
    }

    @Test
    @DisplayName("refresh - optimistic locking collision returns 409 Conflict")
    void refresh_concurrencyCollision_returns409() throws Exception {
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("valid-token");

        Mockito.when(authService.refreshToken(any(TokenRefreshRequestDto.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(RefreshToken.class, 1L));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Concurrent Modification"))
                .andExpect(jsonPath("$.detail").value("Concurrent modification detected. Please try again."));
    }

    @Test
    @DisplayName("logout - success returns 204 No Content")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .principal(() -> "010-1234-5678"))
                .andExpect(status().isNoContent());

        Mockito.verify(authService).logout("010-1234-5678");
    }
}
