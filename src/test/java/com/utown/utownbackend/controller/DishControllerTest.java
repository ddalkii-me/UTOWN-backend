package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.service.DishService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DishController.class)
class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DishService dishService;

    private DishRequestDto requestDto;
    private DishResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createDishRequestDto(1L, 1L, "Pizza");
        responseDto = TestDataFactory.createDishResponseDto(1L, 1L, 1L, "Pizza");
    }

    @Test
    @DisplayName("POST /api/dishes - should return 201")
    void createDish_shouldReturn201() throws Exception {
        when(dishService.createDish(any(DishRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Pizza"));
    }

    @Test
    @DisplayName("POST /api/dishes - should return 400 when name is blank")
    void createDish_shouldReturn400WhenNameBlank() throws Exception {
        DishRequestDto invalidRequest = new DishRequestDto(1L, 1L, "", BigDecimal.valueOf(10.0), "Tasty", "url", DishStatus.AVAILABLE, 1);

        mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/dishes - should return 404 when entity not found")
    void createDish_shouldReturn404WhenEntityNotFound() throws Exception {
        when(dishService.createDish(any(DishRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(post("/api/dishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/dishes - should return 200")
    void getDishes_shouldReturn200() throws Exception {
        when(dishService.getDishes(any(), anyBoolean())).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/dishes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dishes/{id} - should return 200")
    void getDishById_shouldReturn200() throws Exception {
        when(dishService.getDishById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/dishes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dishes/{id} - should return 404 when not found")
    void getDishById_shouldReturn404WhenNotFound() throws Exception {
        when(dishService.getDishById(99L))
                .thenThrow(new EntityNotFoundException("Dish not found"));

        mockMvc.perform(get("/api/dishes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dishes/{id} - should return 200")
    void updateDishById_shouldReturn200() throws Exception {
        when(dishService.updateDish(eq(1L), any(DishRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/dishes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/dishes/{id} - should return 404 when not found")
    void updateDishById_shouldReturn404WhenNotFound() throws Exception {
        when(dishService.updateDish(eq(99L), any(DishRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Dish not found"));

        mockMvc.perform(put("/api/dishes/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dishes/{id} - should return 400 when name is blank")
    void updateDishById_shouldReturn400WhenNameBlank() throws Exception {
        DishRequestDto invalidRequest = new DishRequestDto(1L, 1L, "", BigDecimal.valueOf(10.0), "Tasty", "url", DishStatus.AVAILABLE, 1);

        mockMvc.perform(put("/api/dishes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/dishes/{id} - should return 204")
    void deleteDishById_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/dishes/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(dishService).deleteDish(1L);
    }

    @Test
    @DisplayName("DELETE /api/dishes/{id} - should return 500 when active option groups exist")
    void deleteDishById_shouldReturn500WhenOptionGroupsExist() throws Exception {
        doThrow(new IllegalStateException("Cannot delete Dish while it has active Option Groups"))
                .when(dishService).deleteDish(1L);

        mockMvc.perform(delete("/api/dishes/{id}", 1L))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("DELETE /api/dishes/{id} - should return 404 when not found")
    void deleteDishById_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Dish not found"))
                .when(dishService).deleteDish(99L);

        mockMvc.perform(delete("/api/dishes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dishes/{id}/restore - should return 204")
    void restoreDishById_shouldReturn204() throws Exception {
        mockMvc.perform(put("/api/dishes/{id}/restore", 1L))
                .andExpect(status().isNoContent());

        verify(dishService).restoreDish(1L);
    }

    @Test
    @DisplayName("PUT /api/dishes/{id}/restore - should return 404 when not found")
    void restoreDishById_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Deleted dish not found"))
                .when(dishService).restoreDish(99L);

        mockMvc.perform(put("/api/dishes/{id}/restore", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
