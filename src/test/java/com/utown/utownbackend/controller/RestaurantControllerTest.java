package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.dto.WorkingHoursDto;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.service.RestaurantService;
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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RestaurantService restaurantService;

    @MockitoBean
    private com.utown.utownbackend.service.MenuService menuService;

    private RestaurantRequestDto requestDto;
    private RestaurantResponseDto responseDto;
    private WorkingHoursDto workingHoursDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createRestaurantRequestDto(1L, 1L, 1L, "KFC");
        responseDto = TestDataFactory.createRestaurantResponseDto(1L, 1L, 1L, 1L, "KFC");
        workingHoursDto = TestDataFactory.createWorkingHoursDto(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("POST /api/restaurants - should return 201")
    void createRestaurant_shouldReturn201() throws Exception {
        when(restaurantService.createRestaurant(any(RestaurantRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("KFC"));
    }

    @Test
    @DisplayName("POST /api/restaurants - should return 400 when name is blank")
    void createRestaurant_shouldReturn400WhenNameBlank() throws Exception {
        RestaurantRequestDto invalidRequest = new RestaurantRequestDto(1L, 1L, 1L, "", "desc", "addr", "1234", null, null, null, null, RestaurantStatus.OPEN);

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/restaurants - should return 404 when entity not found")
    void createRestaurant_shouldReturn404WhenEntityNotFound() throws Exception {
        when(restaurantService.createRestaurant(any(RestaurantRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Owner not found"));

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/restaurants - should return 200")
    void getAllRestaurants_shouldReturn200() throws Exception {
        when(restaurantService.getAllRestaurants()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id} - should return 200")
    void getRestaurantById_shouldReturn200() throws Exception {
        when(restaurantService.getRestaurantById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/restaurants/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id} - should return 404 when not found")
    void getRestaurantById_shouldReturn404WhenNotFound() throws Exception {
        when(restaurantService.getRestaurantById(99L))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(get("/api/restaurants/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/menu - should return 200 with full menu hierarchy")
    void getRestaurantMenu_shouldReturn200() throws Exception {
        var menuDto = new com.utown.utownbackend.dto.RestaurantMenuResponseDto(
                1L,
                "KFC",
                RestaurantStatus.OPEN,
                new BigDecimal("15000"),
                List.of(
                        new com.utown.utownbackend.dto.MenuCategoryDto(
                                10L,
                                "Chicken",
                                "Crispy",
                                "url",
                                1,
                                List.of(
                                        new com.utown.utownbackend.dto.MenuDishDto(
                                                100L,
                                                "Hot Wings",
                                                "Spicy wings",
                                                new BigDecimal("8000"),
                                                "url",
                                                com.utown.utownbackend.entity.DishStatus.AVAILABLE,
                                                1,
                                                List.of()
                                        )
                                )
                        )
                )
        );

        when(menuService.getRestaurantMenu(1L)).thenReturn(menuDto);

        mockMvc.perform(get("/api/restaurants/{id}/menu", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(1L))
                .andExpect(jsonPath("$.restaurantName").value("KFC"))
                .andExpect(jsonPath("$.restaurantStatus").value("OPEN"))
                .andExpect(jsonPath("$.categories[0].id").value(10L))
                .andExpect(jsonPath("$.categories[0].name").value("Chicken"))
                .andExpect(jsonPath("$.categories[0].dishes[0].id").value(100L))
                .andExpect(jsonPath("$.categories[0].dishes[0].name").value("Hot Wings"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/menu - should return 404 when restaurant not found")
    void getRestaurantMenu_shouldReturn404WhenNotFound() throws Exception {
        when(menuService.getRestaurantMenu(99L))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(get("/api/restaurants/{id}/menu", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/working-hours - should return 200")
    void getWorkingHoursById_shouldReturn200() throws Exception {
        when(restaurantService.getWorkingHours(1L)).thenReturn(List.of(workingHoursDto));

        mockMvc.perform(get("/api/restaurants/{id}/working-hours", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @DisplayName("GET /api/restaurants/{id}/working-hours - should return 404 when restaurant not found")
    void getWorkingHoursById_shouldReturn404WhenNotFound() throws Exception {
        when(restaurantService.getWorkingHours(99L))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(get("/api/restaurants/{id}/working-hours", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id}/working-hours/{dayOfWeek} - should return 204")
    void updateWorkingHourForDay_shouldReturn204() throws Exception {
        mockMvc.perform(put("/api/restaurants/{id}/working-hours/{dayOfWeek}", 1L, "MONDAY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workingHoursDto)))
                .andExpect(status().isNoContent());

        verify(restaurantService).updateWorkingHourForDay(eq(1L), eq(DayOfWeek.MONDAY), any(WorkingHoursDto.class));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id} - should return 200")
    void updateRestaurant_shouldReturn200() throws Exception {
        when(restaurantService.updateRestaurant(eq(1L), any(RestaurantRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/restaurants/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id} - should return 404 when not found")
    void updateRestaurant_shouldReturn404WhenNotFound() throws Exception {
        when(restaurantService.updateRestaurant(eq(99L), any(RestaurantRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Restaurant not found"));

        mockMvc.perform(put("/api/restaurants/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id} - should return 400 when name is blank")
    void updateRestaurant_shouldReturn400WhenNameBlank() throws Exception {
        RestaurantRequestDto invalidRequest = new RestaurantRequestDto(1L, 1L, 1L, "", "desc", "addr", "1234", null, null, null, null, RestaurantStatus.OPEN);

        mockMvc.perform(put("/api/restaurants/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/restaurants/{id}/working-hours/{dayOfWeek} - should return 400 when dayOfWeek is null")
    void updateWorkingHours_shouldReturn400WhenDayOfWeekNull() throws Exception {
        WorkingHoursDto invalidDto = new WorkingHoursDto(null, LocalTime.of(9, 0), LocalTime.of(22, 0), false);

        mockMvc.perform(put("/api/restaurants/{id}/working-hours/{dayOfWeek}", 1L, "MONDAY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{id} - should return 204")
    void deleteRestaurant_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/restaurants/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(restaurantService).deleteRestaurant(1L);
    }

    @Test
    @DisplayName("DELETE /api/restaurants/{id} - should return 404 when not found")
    void deleteRestaurant_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Restaurant not found"))
                .when(restaurantService).deleteRestaurant(99L);

        mockMvc.perform(delete("/api/restaurants/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
