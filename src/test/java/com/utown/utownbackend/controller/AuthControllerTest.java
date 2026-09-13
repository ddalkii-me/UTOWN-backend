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
    @DisplayName("logout - success returns 204 No Content")
    void logout_success() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .principal(() -> "010-1234-5678"))
                .andExpect(status().isNoContent());

        Mockito.verify(authService).logout("010-1234-5678");
    }
}
