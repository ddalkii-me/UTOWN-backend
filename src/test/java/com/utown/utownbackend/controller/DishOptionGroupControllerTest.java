package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;
import com.utown.utownbackend.service.DishOptionGroupService;
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
@WebMvcTest(DishOptionGroupController.class)
class DishOptionGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DishOptionGroupService dishOptionGroupService;

    private DishOptionGroupRequestDto requestDto;
    private DishOptionGroupResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createDishOptionGroupRequestDto(1L, "Size");
        responseDto = TestDataFactory.createDishOptionGroupResponseDto(1L, 1L, "Size");
    }

    @Test
    @DisplayName("POST /api/dish-option-groups - should return 201")
    void createDishOptionGroup_shouldReturn201() throws Exception {
        when(dishOptionGroupService.createDishOptionGroup(any(DishOptionGroupRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/dish-option-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Size"));
    }

    @Test
    @DisplayName("POST /api/dish-option-groups - should return 400 when dishId is null")
    void createDishOptionGroup_shouldReturn400WhenDishIdNull() throws Exception {
        DishOptionGroupRequestDto invalidRequest = new DishOptionGroupRequestDto(null, "Size", true, 1, 1, 1);

        mockMvc.perform(post("/api/dish-option-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/dish-option-groups - should return 200")
    void getAllDishOptionGroups_shouldReturn200() throws Exception {
        when(dishOptionGroupService.getAllDishOptionGroups()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/dish-option-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dish-option-groups/dish/{dishId} - should return 200")
    void getDishOptionGroupsByDishId_shouldReturn200() throws Exception {
        when(dishOptionGroupService.getDishOptionGroupsByDishId(1L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/dish-option-groups/dish/{dishId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dish-option-groups/{id} - should return 200")
    void getDishOptionGroupById_shouldReturn200() throws Exception {
        when(dishOptionGroupService.getDishOptionGroupById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/dish-option-groups/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/dish-option-groups/{id} - should return 404 when not found")
    void getDishOptionGroupById_shouldReturn404WhenNotFound() throws Exception {
        when(dishOptionGroupService.getDishOptionGroupById(99L))
                .thenThrow(new EntityNotFoundException("Dish Option Group not found"));

        mockMvc.perform(get("/api/dish-option-groups/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dish-option-groups/{id} - should return 200")
    void updateDishOptionGroup_shouldReturn200() throws Exception {
        when(dishOptionGroupService.updateDishOptionGroup(eq(1L), any(DishOptionGroupRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/dish-option-groups/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/dish-option-groups/{id} - should return 404 when not found")
    void updateDishOptionGroup_shouldReturn404WhenNotFound() throws Exception {
        when(dishOptionGroupService.updateDishOptionGroup(eq(99L), any(DishOptionGroupRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Dish Option Group not found"));

        mockMvc.perform(put("/api/dish-option-groups/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/dish-option-groups/{id} - should return 400 when name is blank")
    void updateDishOptionGroup_shouldReturn400WhenNameBlank() throws Exception {
        DishOptionGroupRequestDto invalidRequest = new DishOptionGroupRequestDto(1L, "", true, 1, 1, 1);

        mockMvc.perform(put("/api/dish-option-groups/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/dish-option-groups/{id} - should return 204")
    void deleteDishOptionGroup_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/dish-option-groups/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(dishOptionGroupService).deleteDishOptionGroup(1L);
    }

    @Test
    @DisplayName("DELETE /api/dish-option-groups/{id} - should return 404 when not found")
    void deleteDishOptionGroup_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Dish Option Group not found"))
                .when(dishOptionGroupService).deleteDishOptionGroup(99L);

        mockMvc.perform(delete("/api/dish-option-groups/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
