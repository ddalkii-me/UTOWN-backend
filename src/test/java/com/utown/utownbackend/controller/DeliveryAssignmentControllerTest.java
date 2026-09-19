package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.security.DeliveryAssignmentSecurity;
import com.utown.utownbackend.security.OrderSecurity;
import com.utown.utownbackend.service.DeliveryAssignmentService;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DeliveryAssignmentController.class)
@Import(DeliveryAssignmentControllerTest.MethodSecurityTestConfig.class)
@WithMockUser(roles = "ADMIN")
class DeliveryAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryAssignmentService deliveryAssignmentService;

    @MockitoBean
    private DeliveryAssignmentSecurity deliveryAssignmentSecurity;

    @MockitoBean
    private OrderSecurity orderSecurity;

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

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        @Primary
        DeliveryAssignmentSecurity deliveryAssignmentSecurity() {
            return mock(DeliveryAssignmentSecurity.class);
        }

        @Bean
        @Primary
        OrderSecurity orderSecurity() {
            return mock(OrderSecurity.class);
        }
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /api/delivery-assignments - CUSTOMER should be forbidden")
    void createAssignment_customer_shouldReturn403() throws Exception {

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("POST /api/delivery-assignments - RIDER should be forbidden")
    void createAssignment_rider_shouldReturn403() throws Exception {

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("POST /api/delivery-assignments - restaurant owner should create assignment for own restaurant")
    void createAssignment_restaurantOwner_shouldReturn201() throws Exception {

        when(orderSecurity.isRestaurantOwner(any(), eq(100L)))
                .thenReturn(true);

        when(deliveryAssignmentService.createAssignment(any(DeliveryAssignmentRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("POST /api/delivery-assignments - restaurant owner should not assign another restaurant's order")
    void createAssignment_restaurantOwnerOtherRestaurant_shouldReturn403() throws Exception {

        when(orderSecurity.isRestaurantOwner(any(), eq(100L)))
                .thenReturn(false);

        mockMvc.perform(post("/api/delivery-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("GET /api/delivery-assignments/{id} - restaurant owner can view own restaurant assignment")
    void getAssignmentById_restaurantOwnerOwnRestaurant_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRestaurantOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.getAssignmentById(50L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("GET /api/delivery-assignments/{id} - restaurant owner cannot view another restaurant assignment")
    void getAssignmentById_restaurantOwnerOtherRestaurant_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRestaurantOwner(eq(50L), any()))
                .thenReturn(false);

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("GET /api/delivery-assignments/{id} - rider can view own assignment")
    void getAssignmentById_riderOwnAssignment_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.getAssignmentById(50L))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("GET /api/delivery-assignments/{id} - rider cannot view another rider assignment")
    void getAssignmentById_riderOtherAssignment_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/delivery-assignments/{id} - customer should be forbidden")
    void getAssignmentById_customer_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/delivery-assignments/{id}", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - assigned rider should return 200")
    void acceptAssignment_riderOwner_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.acceptAssignment(50L))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - different rider should return 403")
    void acceptAssignment_differentRider_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - restaurant owner should return 403")
    void acceptAssignment_restaurantOwner_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/accept - customer should return 403")
    void acceptAssignment_customer_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/accept", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/pickup - assigned rider should return 200")
    void pickupDelivery_riderOwner_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.pickupDelivery(50L))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/pickup", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/pickup - different rider should return 403")
    void pickupDelivery_differentRider_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/pickup", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/pickup - restaurant owner should return 403")
    void pickupDelivery_restaurantOwner_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/pickup", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/pickup - customer should return 403")
    void pickupDelivery_customer_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/pickup", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/deliver - assigned rider should return 200")
    void completeDelivery_riderOwner_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.completeDelivery(50L))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/deliver", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/deliver - different rider should return 403")
    void completeDelivery_differentRider_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/deliver", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/deliver - restaurant owner should return 403")
    void completeDelivery_restaurantOwner_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/deliver", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/deliver - customer should return 403")
    void completeDelivery_customer_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/deliver", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/cancel - assigned rider should return 200")
    void cancelAssignment_riderOwner_shouldReturn200() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(true);

        when(deliveryAssignmentService.cancelAssignment(50L))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/cancel", 50L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50L));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/cancel - different rider should return 403")
    void cancelAssignment_differentRider_shouldReturn403() throws Exception {

        when(deliveryAssignmentSecurity.isRiderOwner(eq(50L), any()))
                .thenReturn(false);

        mockMvc.perform(patch("/api/delivery-assignments/{id}/cancel", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/cancel - restaurant owner should return 403")
    void cancelAssignment_restaurantOwner_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/cancel", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("PATCH /api/delivery-assignments/{id}/cancel - customer should return 403")
    void cancelAssignment_customer_shouldReturn403() throws Exception {

        mockMvc.perform(patch("/api/delivery-assignments/{id}/cancel", 50L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("GET /api/delivery-assignments - rider should be forbidden")
    void getAssignments_rider_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/delivery-assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    @DisplayName("GET /api/delivery-assignments - restaurant owner should be forbidden")
    void getAssignments_restaurantOwner_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/delivery-assignments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/delivery-assignments - customer should be forbidden")
    void getAssignments_customer_shouldReturn403() throws Exception {

        mockMvc.perform(get("/api/delivery-assignments"))
                .andExpect(status().isForbidden());
    }
}
