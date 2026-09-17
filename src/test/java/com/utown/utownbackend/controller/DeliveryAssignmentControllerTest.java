package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.service.DeliveryAssignmentService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DeliveryAssignmentController.class)
class DeliveryAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryAssignmentService deliveryAssignmentService;

    private DeliveryAssignmentRequestDto requestDto;
    private DeliveryAssignmentResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createDeliveryAssignmentRequestDto(100L, 10L);
        responseDto = TestDataFactory.createDeliveryAssignmentResponseDto(50L, 100L, 10L);
    }

    @Test
    @DisplayName("POST /api/delivery-assignments - should return 201 Created")
    void createAssignment_shouldReturn201() throws Exception {
        when(deliveryAssignmentService.createAssignment(any(DeliveryAssignmentRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.orderId").value(100L))
                .andExpect(jsonPath("$.riderId").value(10L))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("POST /api/delivery-assignments - validation error when orderId missing")
    void createAssignment_missingOrderId_shouldReturn400() throws Exception {
        DeliveryAssignmentRequestDto invalid = new DeliveryAssignmentRequestDto(null, 10L);

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("POST /api/delivery-assignments - order already has active assignment returns 409")
    void createAssignment_conflict_shouldReturn409() throws Exception {
        when(deliveryAssignmentService.createAssignment(any(DeliveryAssignmentRequestDto.class)))
                .thenThrow(new ResourceConflictException("Order already has an active delivery assignment: 100"));

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @DisplayName("GET /api/delivery-assignments/{id} - should return 200 OK when found")
    void getAssignmentById_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.getAssignmentById(50L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L))
                .andExpect(jsonPath("$.orderId").value(100L));
    }

    @Test
    @DisplayName("GET /api/delivery-assignments/{id} - should return 404 Not Found")
    void getAssignmentById_notFound_shouldReturn404() throws Exception {
        when(deliveryAssignmentService.getAssignmentById(999L))
                .thenThrow(new EntityNotFoundException("Delivery assignment not found with id: 999"));

        mockMvc.perform(get("/api/delivery-assignments/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/delivery-assignments - should return 200 OK with list")
    void getAssignments_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.getAssignments(null, null, null)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/delivery-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(50L));
    }

    @Test
    @DisplayName("GET /api/delivery-assignments with filters - should pass query params")
    void getAssignments_withFilters_shouldPassParams() throws Exception {
        when(deliveryAssignmentService.getAssignments(10L, 100L, DeliveryAssignmentStatus.ASSIGNED))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/delivery-assignments")
                        .param("riderId", "10")
                        .param("orderId", "100")
                        .param("status", "ASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(deliveryAssignmentService).getAssignments(10L, 100L, DeliveryAssignmentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - should return 200 OK")
    void acceptAssignment_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.acceptAssignment(50L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - illegal state returns 400 Bad Request")
    void acceptAssignment_illegalState_shouldReturn400() throws Exception {
        when(deliveryAssignmentService.acceptAssignment(50L))
                .thenThrow(new IllegalStateException("Cannot accept assignment in status: DELIVERED"));

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("PATCH /api/delivery-assignments/{id}/pickup - should return 200 OK")
    void pickupDelivery_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.pickupDelivery(50L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/pickup", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @DisplayName("PATCH /api/delivery-assignments/{id}/deliver - should return 200 OK")
    void completeDelivery_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.completeDelivery(50L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/deliver", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @DisplayName("PATCH /api/delivery-assignments/{id}/cancel - should return 200 OK")
    void cancelAssignment_shouldReturn200() throws Exception {
        when(deliveryAssignmentService.cancelAssignment(50L)).thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/cancel", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }
}
