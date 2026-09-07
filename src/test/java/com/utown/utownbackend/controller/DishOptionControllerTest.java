package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.entity.DishOptionStatus;
import com.utown.utownbackend.service.DishOptionService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DishOptionController.class)
class DishOptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DishOptionService dishOptionService;

    private DishOptionRequestDto requestDto;
    private DishOptionResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createDishOptionRequestDto(1L, "Extra Cheese");
        responseDto = TestDataFactory.createDishOptionResponseDto(1L, 1L, "Extra Cheese");
    }

    @Test
    @DisplayName("POST /api/dish-options - should return 201")
    void createDishOption_shouldReturn201() throws Exception {
        when(dishOptionService.createDishOption(any(DishOptionRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/dish-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Extra Cheese"));
    }

    @Test
    @DisplayName("POST /api/dish-options - should return 400 when name is blank")
    void createDishOption_shouldReturn400WhenNameBlank() throws Exception {
        DishOptionRequestDto invalidRequest = new DishOptionRequestDto("", 1L, BigDecimal.valueOf(1.50), 1, DishOptionStatus.AVAILABLE);

        mockMvc.perform(post("/api/dish-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/dish-options - should return 200")
    void getAllDishOptions_shouldReturn200() throws Exception {
        when(dishOptionService.getAllDishOptions()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/dish-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dish-options/{id} - should return 200")
    void getDishOptionById_shouldReturn200() throws Exception {
        when(dishOptionService.getDishOptionById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/dish-options/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dish-options/{id} - should return 404 when not found")
    void getDishOptionById_shouldReturn404WhenNotFound() throws Exception {
        when(dishOptionService.getDishOptionById(99L))
                .thenThrow(new EntityNotFoundException("Dish Option not found"));

        mockMvc.perform(get("/api/dish-options/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/dish-options/group/{optionGroupId} - should return 200")
    void getDishOptionsByGroupId_shouldReturn200() throws Exception {
        when(dishOptionService.getDishOptionsByGroupId(1L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/dish-options/group/{optionGroupId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/dish-options/{id} - should return 200")
    void updateDishOption_shouldReturn200() throws Exception {
        when(dishOptionService.updateDishOption(eq(1L), any(DishOptionRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/dish-options/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/dish-options/{id} - should return 404 when not found")
    void updateDishOption_shouldReturn404WhenNotFound() throws Exception {
        when(dishOptionService.updateDishOption(eq(99L), any(DishOptionRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Dish Option not found"));

        mockMvc.perform(put("/api/dish-options/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dish-options/{id} - should return 400 when name is blank")
    void updateDishOption_shouldReturn400WhenNameBlank() throws Exception {
        DishOptionRequestDto invalidRequest = new DishOptionRequestDto("", 1L, BigDecimal.valueOf(1.50), 1, DishOptionStatus.AVAILABLE);

        mockMvc.perform(put("/api/dish-options/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/dish-options/{id} - should return 204")
    void deleteDishOption_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/dish-options/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(dishOptionService).deleteDishOption(1L);
    }

    @Test
    @DisplayName("DELETE /api/dish-options/{id} - should return 404 when not found")
    void deleteDishOption_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Dish Option not found"))
                .when(dishOptionService).deleteDishOption(99L);

        mockMvc.perform(delete("/api/dish-options/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
