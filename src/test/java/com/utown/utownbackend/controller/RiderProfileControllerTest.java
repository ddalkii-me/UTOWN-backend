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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RiderProfileController.class)
class RiderProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RiderProfileService riderProfileService;

    private RiderProfileRequestDto requestDto;
    private RiderProfileResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createRiderProfileRequestDto(1L, TransportType.MOTORCYCLE);
        responseDto = TestDataFactory.createRiderProfileResponseDto(10L, 1L);
    }

    @Test
    @DisplayName("POST /api/riders - should return 201 Created")
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
    @DisplayName("GET /api/riders - should return 200 OK with list")
    void getAllRiderProfiles_shouldReturn200() throws Exception {
        when(riderProfileService.getAllRiderProfiles(null, null)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/riders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    @DisplayName("GET /api/riders with query params - should pass params and return 200 OK")
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
    @DisplayName("GET /api/riders/{id} - should return 200 OK when found")
    void getRiderProfileById_shouldReturn200() throws Exception {
        when(riderProfileService.getRiderProfileById(10L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("GET /api/riders/{id} - should return 404 Not Found")
    void getRiderProfileById_notFound_shouldReturn404() throws Exception {
        when(riderProfileService.getRiderProfileById(99L))
                .thenThrow(new EntityNotFoundException("Rider profile not found with id: 99"));

        mockMvc.perform(get("/api/riders/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/riders/user/{userId} - should return 200 OK when found")
    void getRiderProfileByUserId_shouldReturn200() throws Exception {
        when(riderProfileService.getRiderProfileByUserId(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/riders/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @DisplayName("PUT /api/riders/{id} - should return 200 OK")
    void updateRiderProfile_shouldReturn200() throws Exception {
        RiderProfileUpdateRequestDto updateDto = new RiderProfileUpdateRequestDto(TransportType.CAR);

        when(riderProfileService.updateRiderProfile(eq(10L), any(RiderProfileUpdateRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/riders/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("PATCH /api/riders/{id}/availability - should return 200 OK")
    void updateAvailability_shouldReturn200() throws Exception {
        RiderAvailabilityUpdateRequestDto availDto = new RiderAvailabilityUpdateRequestDto(false);

        when(riderProfileService.updateAvailability(eq(10L), any(RiderAvailabilityUpdateRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/riders/{id}/availability", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("PATCH /api/riders/{id}/status - should return 200 OK")
    void updateStatus_shouldReturn200() throws Exception {
        RiderStatusUpdateRequestDto statusDto = new RiderStatusUpdateRequestDto(RiderStatus.SUSPENDED);

        when(riderProfileService.updateStatus(eq(10L), any(RiderStatusUpdateRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/riders/{id}/status", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));
    }

    @Test
    @DisplayName("DELETE /api/riders/{id} - should return 204 No Content")
    void deleteRiderProfile_shouldReturn204() throws Exception {
        doNothing().when(riderProfileService).deleteRiderProfile(10L);

        mockMvc.perform(delete("/api/riders/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(riderProfileService).deleteRiderProfile(10L);
    }
}
