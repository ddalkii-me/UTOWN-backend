package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.service.CityService;
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
@WebMvcTest(CityController.class)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CityService cityService;

    private CityRequestDto requestDto;
    private CityResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createCityRequestDto("Seoul");
        responseDto = TestDataFactory.createCityResponseDto(1L, "Seoul");
    }

    @Test
    @DisplayName("POST /api/cities - should return 201")
    void createCity_shouldReturn201() throws Exception {
        when(cityService.createCity(any(CityRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Seoul"));
    }

    @Test
    @DisplayName("POST /api/cities - should return 409 when name is duplicate")
    void createCity_shouldReturn409WhenDuplicate() throws Exception {
        when(cityService.createCity(any(CityRequestDto.class)))
                .thenThrow(new ResourceConflictException("City name already exists"));

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }

    @Test
    @DisplayName("POST /api/cities - should return 400 when name is blank")
    void createCity_shouldReturn400WhenNameBlank() throws Exception {
        CityRequestDto invalidRequest = new CityRequestDto("");

        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/cities - should return 200")
    void getAllCities_shouldReturn200() throws Exception {
        when(cityService.getAllCities()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/cities/{id} - should return 200")
    void getCityById_shouldReturn200() throws Exception {
        when(cityService.getCityById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/cities/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/cities/{id} - should return 404 when not found")
    void getCityById_shouldReturn404WhenNotFound() throws Exception {
        when(cityService.getCityById(99L))
                .thenThrow(new EntityNotFoundException("City not found"));

        mockMvc.perform(get("/api/cities/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/cities/{id} - should return 200")
    void updateCity_shouldReturn200() throws Exception {
        when(cityService.updateCity(eq(1L), any(CityRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/cities/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/cities/{id} - should return 404 when not found")
    void updateCity_shouldReturn404WhenNotFound() throws Exception {
        when(cityService.updateCity(eq(99L), any(CityRequestDto.class)))
                .thenThrow(new EntityNotFoundException("City not found"));

        mockMvc.perform(put("/api/cities/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/cities/{id} - should return 400 when name is blank")
    void updateCity_shouldReturn400WhenNameBlank() throws Exception {
        CityRequestDto invalidRequest = new CityRequestDto("");

        mockMvc.perform(put("/api/cities/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/cities/{id} - should return 204")
    void deleteCity_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/cities/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(cityService).deleteCity(1L);
    }

    @Test
    @DisplayName("DELETE /api/cities/{id} - should return 404 when not found")
    void deleteCity_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("City not found"))
                .when(cityService).deleteCity(99L);

        mockMvc.perform(delete("/api/cities/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("DELETE /api/cities/{id} - should return 409 when active restaurants exist")
    void deleteCity_shouldReturn409WhenRestaurantsExist() throws Exception {
        doThrow(new ResourceConflictException("Cannot delete city because it still has active restaurants."))
                .when(cityService).deleteCity(1L);

        mockMvc.perform(delete("/api/cities/{id}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }
}
