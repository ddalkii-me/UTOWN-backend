package com.utown.utownbackend.integration;

import com.utown.utownbackend.service.SocketIONotificationService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private SocketIONotificationService socketIONotificationService;

    @Test
    @DisplayName("POST /api/auth/register - successfully creates user and returns JWT token")
    void register_success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "010-1234-5678",
                "securePass123",
                "securePass123",
                "Hong Gildong",
                "gildong@example.com",
                "123456"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        AuthResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponseDto.class
        );

        assertThat(response.token()).isNotBlank();
        assertThat(jwtUtil.extractUsername(response.token())).isEqualTo("+821012345678");

        Optional<User> savedUser = userRepository.findByPhoneAndDeletedAtIsNull("+821012345678");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getEmail()).isEqualTo("gildong@example.com");
        assertThat(savedUser.get().getName()).isEqualTo("Hong Gildong");
    }

    @Test
    @DisplayName("POST /api/auth/register - fails validation with invalid phone format")
    void register_invalidPhone_returnsBadRequest() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto(
                "invalid-phone",
                "strongPassword123",
                "strongPassword123",
                "Hong Gildong",
                "gildong@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - successfully authenticates and returns JWT token")
    void login_success() throws Exception {
        // First register
        RegisterRequestDto registerRequest = new RegisterRequestDto(
                "010-9876-5432",
                "loginPass123",
                "loginPass123",
                "Login Test User",
                "login.test@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login with formatted phone
        LoginRequestDto loginRequest = new LoginRequestDto("010-9876-5432", "loginPass123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        AuthResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponseDto.class
        );

        assertThat(response.token()).isNotBlank();
        assertThat(jwtUtil.extractUsername(response.token())).isEqualTo("+821098765432");
    }

    @Test
    @DisplayName("POST /api/auth/register - returns 409 Conflict when phone already registered")
    void register_duplicatePhone_returnsConflict() throws Exception {
        RegisterRequestDto first = new RegisterRequestDto(
                "010-5555-4444",
                "password123",
                "password123",
                "User One",
                "one@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequestDto duplicate = new RegisterRequestDto(
                "010-5555-4444",
                "conflictPass123",
                "conflictPass123",
                "Conflict Test 2",
                "conflict2@example.com",
                "123456"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }
}
