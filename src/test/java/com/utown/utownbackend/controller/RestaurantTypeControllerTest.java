package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.service.RestaurantTypeService;
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
@WebMvcTest(RestaurantTypeController.class)
class RestaurantTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantTypeService restaurantTypeService;

    private RestaurantTypeRequestDto requestDto;
    private RestaurantTypeResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createRestaurantTypeRequestDto("Fast Food");
        responseDto = TestDataFactory.createRestaurantTypeResponseDto(1L, "Fast Food");
    }

    @Test
    @DisplayName("POST /api/restaurant-types - should return 201")
    void createRestaurantType_shouldReturn201() throws Exception {
        when(restaurantTypeService.createRestaurantType(any(RestaurantTypeRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/restaurant-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Fast Food"));
    }

    @Test
    @DisplayName("POST /api/restaurant-types - should return 400 when name is blank")
    void createRestaurantType_shouldReturn400WhenNameBlank() throws Exception {
        RestaurantTypeRequestDto invalidRequest = new RestaurantTypeRequestDto("", "Quick");

        mockMvc.perform(post("/api/restaurant-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/restaurant-types - should return 200")
    void getAllRestaurantTypes_shouldReturn200() throws Exception {
        when(restaurantTypeService.getAllRestaurantTypes()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/restaurant-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/restaurant-types/{id} - should return 200")
    void getRestaurantTypeById_shouldReturn200() throws Exception {
        when(restaurantTypeService.getRestaurantTypeById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/restaurant-types/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/restaurant-types/{id} - should return 404 when not found")
    void getRestaurantTypeById_shouldReturn404WhenNotFound() throws Exception {
        when(restaurantTypeService.getRestaurantTypeById(99L))
                .thenThrow(new EntityNotFoundException("Restaurant type not found"));

        mockMvc.perform(get("/api/restaurant-types/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/restaurant-types/{id} - should return 200")
    void updateRestaurantType_shouldReturn200() throws Exception {
        when(restaurantTypeService.updateRestaurantType(eq(1L), any(RestaurantTypeRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/restaurant-types/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/restaurant-types/{id} - should return 404 when not found")
    void updateRestaurantType_shouldReturn404WhenNotFound() throws Exception {
        when(restaurantTypeService.updateRestaurantType(eq(99L), any(RestaurantTypeRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Restaurant type not found"));

        mockMvc.perform(put("/api/restaurant-types/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/restaurant-types/{id} - should return 400 when name is blank")
    void updateRestaurantType_shouldReturn400WhenNameBlank() throws Exception {
        RestaurantTypeRequestDto invalidRequest = new RestaurantTypeRequestDto("", "Quick");

        mockMvc.perform(put("/api/restaurant-types/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/restaurant-types/{id} - should return 204")
    void deleteRestaurantType_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/restaurant-types/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(restaurantTypeService).deleteRestaurantType(1L);
    }

    @Test
    @DisplayName("DELETE /api/restaurant-types/{id} - should return 404 when not found")
    void deleteRestaurantType_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Restaurant type not found"))
                .when(restaurantTypeService).deleteRestaurantType(99L);

        mockMvc.perform(delete("/api/restaurant-types/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
