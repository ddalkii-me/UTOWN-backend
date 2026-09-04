package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.DeliveryAreaRequestDto;
import com.utown.utownbackend.dto.DeliveryAreaResponseDto;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.service.DeliveryAreaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.utown.utownbackend.util.TestDataFactory;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DeliveryAreaController.class)
class DeliveryAreaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeliveryAreaService deliveryAreaService;

    private DeliveryAreaRequestDto requestDto;
    private DeliveryAreaResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createDeliveryAreaRequestDto(1L, "Gangnam");
        responseDto = TestDataFactory.createDeliveryAreaResponseDto(1L, 1L, "Gangnam");
    }

    @Test
    @DisplayName("POST /api/delivery-areas - should return 201")
    void createDeliveryArea_shouldReturn201() throws Exception {
        when(deliveryAreaService.createDeliveryArea(any(DeliveryAreaRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/delivery-areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Gangnam"));
    }

    @Test
    @DisplayName("POST /api/delivery-areas - should return 400 when cityId is null")
    void createDeliveryArea_shouldReturn400WhenCityIdNull() throws Exception {
        DeliveryAreaRequestDto invalidRequest = new DeliveryAreaRequestDto(null, "Gangnam");

        mockMvc.perform(post("/api/delivery-areas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/delivery-areas - should return 200")
    void getAllDeliveryAreas_shouldReturn200() throws Exception {
        when(deliveryAreaService.getAllDeliveryAreas()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/delivery-areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/delivery-areas/{id} - should return 200")
    void getDeliveryAreaById_shouldReturn200() throws Exception {
        when(deliveryAreaService.getDeliveryAreaById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/delivery-areas/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/delivery-areas/{id} - should return 404 when not found")
    void getDeliveryAreaById_shouldReturn404WhenNotFound() throws Exception {
        when(deliveryAreaService.getDeliveryAreaById(99L))
                .thenThrow(new EntityNotFoundException("Delivery area not found"));

        mockMvc.perform(get("/api/delivery-areas/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/delivery-areas/city/{cityId} - should return 200")
    void getDeliveryAreasByCity_shouldReturn200() throws Exception {
        when(deliveryAreaService.getDeliveryAreasByCity(1L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/delivery-areas/city/{cityId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/delivery-areas/{id} - should return 200")
    void updateDeliveryArea_shouldReturn200() throws Exception {
        when(deliveryAreaService.updateDeliveryArea(eq(1L), any(DeliveryAreaRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/delivery-areas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/delivery-areas/{id} - should return 404 when not found")
    void updateDeliveryArea_shouldReturn404WhenNotFound() throws Exception {
        when(deliveryAreaService.updateDeliveryArea(eq(99L), any(DeliveryAreaRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Delivery area not found"));

        mockMvc.perform(put("/api/delivery-areas/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/delivery-areas/{id} - should return 409 when name is duplicate")
    void updateDeliveryArea_shouldReturn409WhenDuplicate() throws Exception {
        when(deliveryAreaService.updateDeliveryArea(eq(1L), any(DeliveryAreaRequestDto.class)))
                .thenThrow(new ResourceConflictException("Delivery area already exists"));

        mockMvc.perform(put("/api/delivery-areas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @DisplayName("PUT /api/delivery-areas/{id} - should return 400 when name is blank")
    void updateDeliveryArea_shouldReturn400WhenNameBlank() throws Exception {
        DeliveryAreaRequestDto invalidRequest = new DeliveryAreaRequestDto(1L, "");

        mockMvc.perform(put("/api/delivery-areas/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/delivery-areas/{id} - should return 204")
    void deleteDeliveryArea_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/delivery-areas/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(deliveryAreaService).deleteDeliveryArea(1L);
    }

    @Test
    @DisplayName("DELETE /api/delivery-areas/{id} - should return 404 when not found")
    void deleteDeliveryArea_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Delivery area not found"))
                .when(deliveryAreaService).deleteDeliveryArea(99L);

        mockMvc.perform(delete("/api/delivery-areas/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/delivery-areas/{id} - should return 409 when active addresses exist")
    void deleteDeliveryArea_shouldReturn409WhenAddressesExist() throws Exception {
        doThrow(new ResourceConflictException("Cannot delete delivery area with active addresses."))
                .when(deliveryAreaService).deleteDeliveryArea(1L);

        mockMvc.perform(delete("/api/delivery-areas/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }
}
