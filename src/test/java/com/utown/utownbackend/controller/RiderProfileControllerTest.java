package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.TransportType;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.service.RiderProfileService;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.security.RiderProfileSecurity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RiderProfileController.class)
@Import(RiderProfileControllerTest.MethodSecurityTestConfig.class)
@WithMockUser(roles = "RIDER")
class RiderProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RiderProfileService riderProfileService;

    @Autowired
    private RiderProfileSecurity riderProfileSecurity;

    private RiderProfileRequestDto requestDto;
    private RiderProfileResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createRiderProfileRequestDto(1L, TransportType.MOTORCYCLE);
        responseDto = TestDataFactory.createRiderProfileResponseDto(10L, 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/riders - admin can create rider profile")
    void createRiderProfile_shouldReturn201() throws Exception {
        when(riderProfileService.createRiderProfile(any(RiderProfileRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/riders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.userName").value("Test Rider"))
                .andExpect(jsonPath("$.transportType").value("MOTORCYCLE"))
                .andExpect(jsonPath("$.availability").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    // NEW: Rider must not be able to create a rider profile.
    @DisplayName("POST /api/riders - rider cannot create rider profile")
    void createRiderProfile_rider_shouldReturn403() throws Exception {

        mockMvc.perform(post("/api/riders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    // CHANGED: Admin because POST /api/riders is ADMIN-only.
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/riders - validation error when userId is missing")
    void createRiderProfile_missingUserId_shouldReturn400() throws Exception {
        RiderProfileRequestDto invalid = new RiderProfileRequestDto(null, TransportType.CAR, true, RiderStatus.ACTIVE);

        mockMvc.perform(post("/api/riders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    // CHANGED: Admin because POST /api/riders is ADMIN-only.
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/riders - user already has profile should return 409 Conflict")
    void createRiderProfile_alreadyExists_shouldReturn409() throws Exception {
        when(riderProfileService.createRiderProfile(any(RiderProfileRequestDto.class)))
                .thenThrow(new ResourceConflictException("Rider profile already exists for user ID: 1"));

        mockMvc.perform(post("/api/riders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/riders - admin can access all rider profiles")
    void getAllRiderProfiles_shouldReturn200() throws Exception {
        when(riderProfileService.getAllRiderProfiles(null, null)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    // NEW
    @DisplayName("GET /api/riders - rider cannot access all rider profiles")
    void getAllRiderProfiles_rider_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/riders"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/riders with query params - admin can filter rider profiles")
    void getAllRiderProfiles_withParams_shouldReturn200() throws Exception {
        when(riderProfileService.getAllRiderProfiles(RiderStatus.ACTIVE, true)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/riders")
                        .param("status", "ACTIVE")
                        .param("availability", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(riderProfileService).getAllRiderProfiles(RiderStatus.ACTIVE, true);
    }

    @Test
    @DisplayName("GET /api/riders/{id} - rider can access own profile")
    void getRiderProfileById_shouldReturn200() throws Exception {
        // NEW: Tell the mocked security helper that profile 10 belongs
        // to the currently authenticated rider.
        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(true);

        when(riderProfileService.getRiderProfileById(10L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService).getRiderProfileById(10L);
    }

    @Test
    // NEW: Rider trying another rider's profile.
    @DisplayName("GET /api/riders/{id} - rider cannot access another rider's profile")
    void getRiderProfileById_otherRider_shouldReturn403() throws Exception {

        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/riders/{id}", 10L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/riders/{id} - admin can access any rider profile")
    void getRiderProfileById_admin_shouldReturn200() throws Exception {

        when(riderProfileService.getRiderProfileById(10L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService).getRiderProfileById(10L);
    }

    @Test
    @DisplayName("GET /api/riders/{id} - rider profile not found should return 404")
    void getRiderProfileById_notFound_shouldReturn404() throws Exception {

        when(riderProfileSecurity.isOwner(eq(99L), any()))
                .thenReturn(true);

        when(riderProfileService.getRiderProfileById(99L))
                .thenThrow(new EntityNotFoundException(
                        "Rider profile not found with id: 99"
                ));

        mockMvc.perform(get("/api/riders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("GET /api/riders/user/{userId} - rider can access own profile")
    void getRiderProfileByUserId_shouldReturn200() throws Exception {
        when(riderProfileSecurity.isOwnerByUserId(eq(1L), any()))
                .thenReturn(true);

        when(riderProfileService.getRiderProfileByUserId(1L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(riderProfileService).getRiderProfileByUserId(1L);
    }

    @Test
    // NEW
    @DisplayName("GET /api/riders/user/{userId} - rider cannot access another user's profile")
    void getRiderProfileByUserId_otherUser_shouldReturn403() throws Exception {

        when(riderProfileSecurity.isOwnerByUserId(eq(2L), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/riders/user/{userId}", 2L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/riders/user/{userId} - admin can access any rider profile")
    void getRiderProfileByUserId_admin_shouldReturn200() throws Exception {

        when(riderProfileService.getRiderProfileByUserId(1L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));

        verify(riderProfileService).getRiderProfileByUserId(1L);
    }

    @Test
    @DisplayName("PUT /api/riders/{id} - rider can update own profile")
    void updateRiderProfile_shouldReturn200() throws Exception {
        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(true);

        RiderProfileUpdateRequestDto updateDto =
                new RiderProfileUpdateRequestDto(TransportType.CAR);

        when(riderProfileService.updateRiderProfile(
                eq(10L),
                any(RiderProfileUpdateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(put("/api/riders/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService)
                .updateRiderProfile(eq(10L), any(RiderProfileUpdateRequestDto.class));
    }

    @Test
    // NEW
    @DisplayName("PUT /api/riders/{id} - rider cannot update another rider's profile")
    void updateRiderProfile_otherRider_shouldReturn403() throws Exception {

        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(false);

        RiderProfileUpdateRequestDto updateDto =
                new RiderProfileUpdateRequestDto(TransportType.CAR);

        mockMvc.perform(put("/api/riders/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/riders/{id} - admin can update rider profile")
    void updateRiderProfile_admin_shouldReturn200() throws Exception {

        RiderProfileUpdateRequestDto updateDto =
                new RiderProfileUpdateRequestDto(TransportType.CAR);

        when(riderProfileService.updateRiderProfile(
                eq(10L),
                any(RiderProfileUpdateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(put("/api/riders/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService)
                .updateRiderProfile(eq(10L), any(RiderProfileUpdateRequestDto.class));
    }

    @Test
    @DisplayName("PATCH /api/riders/{id}/availability - rider can update own availability")
    void updateAvailability_shouldReturn200() throws Exception {
        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(true);

        RiderAvailabilityUpdateRequestDto availDto =
                new RiderAvailabilityUpdateRequestDto(false);

        when(riderProfileService.updateAvailability(
                eq(10L),
                any(RiderAvailabilityUpdateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(patch("/api/riders/{id}/availability", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService)
                .updateAvailability(eq(10L), any(RiderAvailabilityUpdateRequestDto.class));
    }

    @Test
    // NEW
    @DisplayName("PATCH /api/riders/{id}/availability - rider cannot update another rider's availability")
    void updateAvailability_otherRider_shouldReturn403() throws Exception {

        when(riderProfileSecurity.isOwner(eq(10L), any()))
                .thenReturn(false);

        RiderAvailabilityUpdateRequestDto availDto =
                new RiderAvailabilityUpdateRequestDto(false);

        mockMvc.perform(patch("/api/riders/{id}/availability", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/riders/{id}/availability - admin can update availability")
    void updateAvailability_admin_shouldReturn200() throws Exception {

        RiderAvailabilityUpdateRequestDto availDto =
                new RiderAvailabilityUpdateRequestDto(false);

        when(riderProfileService.updateAvailability(
                eq(10L),
                any(RiderAvailabilityUpdateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(patch("/api/riders/{id}/availability", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService)
                .updateAvailability(eq(10L), any(RiderAvailabilityUpdateRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/riders/{id}/status - admin can update rider status")
    void updateStatus_shouldReturn200() throws Exception {
        RiderStatusUpdateRequestDto statusDto =
                new RiderStatusUpdateRequestDto(RiderStatus.SUSPENDED);

        when(riderProfileService.updateStatus(
                eq(10L),
                any(RiderStatusUpdateRequestDto.class)
        )).thenReturn(responseDto);

        mockMvc.perform(patch("/api/riders/{id}/status", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        verify(riderProfileService)
                .updateStatus(eq(10L), any(RiderStatusUpdateRequestDto.class));
    }

    @Test
    // NEW
    @DisplayName("PATCH /api/riders/{id}/status - rider cannot update status")
    void updateStatus_rider_shouldReturn403() throws Exception {

        RiderStatusUpdateRequestDto statusDto =
                new RiderStatusUpdateRequestDto(RiderStatus.SUSPENDED);

        mockMvc.perform(patch("/api/riders/{id}/status", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/riders/{id} - admin can deactivate rider profile")
    void deleteRiderProfile_shouldReturn204() throws Exception {

        doNothing().when(riderProfileService).deleteRiderProfile(10L);

        mockMvc.perform(delete("/api/riders/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(riderProfileService).deleteRiderProfile(10L);
    }

    @Test
    // NEW
    @DisplayName("DELETE /api/riders/{id} - rider cannot deactivate rider profile")
    void deleteRiderProfile_rider_shouldReturn403() throws Exception {

        mockMvc.perform(delete("/api/riders/{id}", 10L))
                .andExpect(status().isForbidden());

        verifyNoInteractions(riderProfileService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        @Primary
        RiderProfileSecurity riderProfileSecurity() {
            return mock(RiderProfileSecurity.class);
        }
    }
}
