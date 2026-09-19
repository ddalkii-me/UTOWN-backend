package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.ChangePasswordRequestDto;
import com.utown.utownbackend.dto.UserProfileResponseDto;
import com.utown.utownbackend.dto.UserProfileUpdateRequestDto;
import com.utown.utownbackend.dto.UserStatusUpdateRequestDto;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.security.UserSecurity;
import com.utown.utownbackend.service.UserService;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import({com.utown.utownbackend.config.SecurityConfig.class, UserSecurity.class})
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserSecurity userSecurity;

    private UserProfileResponseDto profileResponse;
    private final String testPhone = "01012345678";
    private Principal mockPrincipal;

    @BeforeEach
    void setUp() {
        profileResponse = TestDataFactory.createUserProfileResponseDto(1L);
        mockPrincipal = () -> testPhone;
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("GET /api/users/me - should return 200 with current user profile")
    void getCurrentUserProfile_shouldReturn200() throws Exception {
        when(userService.getCurrentUserProfile(testPhone)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/me").principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.phone").value("01000000001"))
                .andExpect(jsonPath("$.name").value("Test User 1"));

        verify(userService).getCurrentUserProfile(testPhone);
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("GET /api/users/me - when user not found returns 404")
    void getCurrentUserProfile_notFound_shouldReturn404() throws Exception {
        when(userService.getCurrentUserProfile(testPhone)).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/me").principal(mockPrincipal))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me - should return 200 with updated profile")
    void updateCurrentUserProfile_shouldReturn200() throws Exception {
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("Updated Name", "updated@example.com");
        when(userService.updateCurrentUserProfile(eq(testPhone), any(UserProfileUpdateRequestDto.class)))
                .thenReturn(profileResponse);

        mockMvc.perform(put("/api/users/me")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).updateCurrentUserProfile(eq(testPhone), any(UserProfileUpdateRequestDto.class));
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me - validation failure should return 400")
    void updateCurrentUserProfile_invalidEmail_shouldReturn400() throws Exception {
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("", "not-an-email");

        mockMvc.perform(put("/api/users/me")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateCurrentUserProfile(any(), any());
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me - email conflict should return 409")
    void updateCurrentUserProfile_emailConflict_shouldReturn409() throws Exception {
        UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("Valid Name", "taken@example.com");
        when(userService.updateCurrentUserProfile(eq(testPhone), any(UserProfileUpdateRequestDto.class)))
                .thenThrow(new ResourceConflictException("Email is already in use"));

        mockMvc.perform(put("/api/users/me")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me/password - valid change should return 204")
    void changePassword_shouldReturn204() throws Exception {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldSecret123", "newSecret123", "newSecret123");
        doNothing().when(userService).changePassword(eq(testPhone), any(ChangePasswordRequestDto.class));

        mockMvc.perform(put("/api/users/me/password")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq(testPhone), any(ChangePasswordRequestDto.class));
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me/password - mismatched confirm password should return 400")
    void changePassword_mismatch_shouldReturn400() throws Exception {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldSecret123", "newSecret123", "different123");

        mockMvc.perform(put("/api/users/me/password")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("PUT /api/users/me/password - wrong current password should return 400")
    void changePassword_wrongCurrentPassword_shouldReturn400() throws Exception {
        ChangePasswordRequestDto request = new ChangePasswordRequestDto("wrongSecret", "newSecret123", "newSecret123");
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(testPhone), any(ChangePasswordRequestDto.class));

        mockMvc.perform(put("/api/users/me/password")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "01012345678")
    @DisplayName("DELETE /api/users/me - should return 204")
    void deleteCurrentUser_shouldReturn204() throws Exception {
        doNothing().when(userService).deleteCurrentUser(testPhone);

        mockMvc.perform(delete("/api/users/me").principal(mockPrincipal))
                .andExpect(status().isNoContent());

        verify(userService).deleteCurrentUser(testPhone);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{id} - should return 200 for admin")
    void getUserById_asAdmin_shouldReturn200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/users/{id} - should return 200 when user is self")
    void getUserById_asSelf_shouldReturn200() throws Exception {
        when(userSecurity.isSelf(any(), eq(1L))).thenReturn(true);
        when(userService.getUserById(1L)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users/{id} - not found should return 404")
    void getUserById_notFound_shouldReturn404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new EntityNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/users - should return 200 with user list for admin")
    void getUsers_asAdmin_shouldReturn200() throws Exception {
        when(userService.getUsers(UserRole.CUSTOMER, UserStatus.ACTIVE)).thenReturn(List.of(profileResponse));

        mockMvc.perform(get("/api/users")
                        .param("role", "CUSTOMER")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(userService).getUsers(UserRole.CUSTOMER, UserStatus.ACTIVE);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/users - should return 403 when caller is not admin")
    void getUsers_asCustomer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUsers(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/users/{id}/status - should return 200 with updated status for admin")
    void updateUserStatus_asAdmin_shouldReturn200() throws Exception {
        UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(UserStatus.SUSPENDED);
        UserProfileResponseDto updatedResponse = new UserProfileResponseDto(
                1L, "01000000001", "Test User 1", "user1@example.com",
                UserRole.CUSTOMER, UserStatus.SUSPENDED, null, null, null, null
        );
        when(userService.updateUserStatus(eq(1L), any(UserStatusUpdateRequestDto.class))).thenReturn(updatedResponse);

        mockMvc.perform(patch("/api/users/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        verify(userService).updateUserStatus(eq(1L), any(UserStatusUpdateRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/users/{id}/status - should return 403 when caller is not admin")
    void updateUserStatus_asCustomer_shouldReturn403() throws Exception {
        UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(UserStatus.SUSPENDED);

        mockMvc.perform(patch("/api/users/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserStatus(any(), any());
    }
}
