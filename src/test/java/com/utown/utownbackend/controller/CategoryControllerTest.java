package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.dto.CategoryResponseDto;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.service.CategoryService;
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
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryRequestDto requestDto;
    private CategoryResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createCategoryRequestDto(1L, "Burgers");
        responseDto = TestDataFactory.createCategoryResponseDto(1L, 1L, "Burgers");
    }

    @Test
    @DisplayName("POST /api/categories - should return 201")
    void createCategory_shouldReturn201() throws Exception {
        when(categoryService.createCategory(any(CategoryRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Burgers"));
    }

    @Test
    @DisplayName("POST /api/categories - should return 400 when name is blank")
    void createCategory_shouldReturn400WhenNameBlank() throws Exception {
        CategoryRequestDto invalidRequest = new CategoryRequestDto(1L, "", "desc", "url", 1);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/categories - should return 404 when restaurant not found")
    void createCategory_shouldReturn404WhenRestaurantNotFound() throws Exception {
        when(categoryService.createCategory(any(CategoryRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/categories - should return 200")
    void getAllCategories_shouldReturn200() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/categories/{id} - should return 200")
    void getCategoryById_shouldReturn200() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/categories/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - should return 200")
    void updateCategory_shouldReturn200() throws Exception {
        when(categoryService.updateCategory(eq(1L), any(CategoryRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - should return 404 when not found")
    void updateCategory_shouldReturn404WhenNotFound() throws Exception {
        when(categoryService.updateCategory(eq(99L), any(CategoryRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Category not found"));

        mockMvc.perform(put("/api/categories/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} - should return 400 when name is blank")
    void updateCategory_shouldReturn400WhenNameBlank() throws Exception {
        CategoryRequestDto invalidRequest = new CategoryRequestDto(1L, "", "desc", "url", 1);

        mockMvc.perform(put("/api/categories/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - should return 204")
    void deleteCategory_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1L);
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - should return 404 when not found")
    void deleteCategory_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Category not found"))
                .when(categoryService).deleteCategory(99L);

        mockMvc.perform(delete("/api/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} - should return 409 when active dishes exist")
    void deleteCategory_shouldReturn409WhenDishesExist() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(responseDto));

        doThrow(new ResourceConflictException("Cannot delete category because it still has active dishes."))
                .when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }
}
