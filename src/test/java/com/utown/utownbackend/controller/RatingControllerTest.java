package com.utown.utownbackend.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.utown.utownbackend.dto.RatingRequestDto;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.RatingResponseDto;
import com.utown.utownbackend.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc(addFilters = false)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RatingService ratingService;

    @Test
    void createRating_shouldReturn201() throws Exception {

        RatingRequestDto request =
                new RatingRequestDto(
                        5,
                        "Great service!"
                );

        RatingResponseDto response =
                new RatingResponseDto(
                        1L,
                        10L,
                        20L,
                        30L,
                        5,
                        "Great service!"
                );

        when(ratingService.createRating(
                eq(10L),
                eq(20L),
                eq(30L),
                any(RatingRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/users/10/restaurants/20/orders/30/rating")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.restaurantId").value(20))
                .andExpect(jsonPath("$.orderId").value(30))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment")
                        .value("Great service!"));

        verify(ratingService).createRating(
                eq(10L),
                eq(20L),
                eq(30L),
                any(RatingRequestDto.class)
        );
    }

    @Test
    void createRating_shouldReturn400WhenScoreIsInvalid() throws Exception {

        RatingRequestDto request =
                new RatingRequestDto(
                        6,
                        "Invalid score"
                );

        mockMvc.perform(
                        post("/api/users/10/restaurants/20/orders/30/rating")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ratingService);
    }

    @Test
    void getRatingsByRestaurant_shouldReturn200() throws Exception {

        RatingResponseDto response =
                new RatingResponseDto(
                        1L,
                        10L,
                        20L,
                        30L,
                        5,
                        "Great service!"
                );

        when(ratingService.getRatingsByRestaurant(20L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/restaurants/20/ratings")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].restaurantId").value(20))
                .andExpect(jsonPath("$[0].score").value(5));

        verify(ratingService)
                .getRatingsByRestaurant(20L);
    }

    @Test
    void getRatingById_shouldReturn200() throws Exception {

        RatingResponseDto response =
                new RatingResponseDto(
                        1L,
                        10L,
                        20L,
                        30L,
                        5,
                        "Great service!"
                );

        when(ratingService.getRatingById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/ratings/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment")
                        .value("Great service!"));

        verify(ratingService)
                .getRatingById(1L);
    }

    @Test
    void deleteRating_shouldReturn204() throws Exception {

        doNothing()
                .when(ratingService)
                .deleteRating(1L);

        mockMvc.perform(
                        delete("/api/ratings/1")
                )
                .andExpect(status().isNoContent());

        verify(ratingService)
                .deleteRating(1L);
    }
}