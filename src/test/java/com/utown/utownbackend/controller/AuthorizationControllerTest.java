package com.utown.utownbackend.controller;

import com.utown.utownbackend.config.SecurityConfig;
import com.utown.utownbackend.dto.RatingResponseDto;
import com.utown.utownbackend.service.RatingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @Test
    @DisplayName("GET /api/ratings/{id} - should return 401 when not authenticated")
    void getRatingById_shouldReturn401WhenNotAuthenticated() throws Exception {

        mockMvc.perform(
                        get("/api/ratings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RIDER")
    @DisplayName("GET /api/ratings/{id} - should return 403 when user has wrong role")
    void getRatingById_shouldReturn403WhenWrongRole() throws Exception {

        mockMvc.perform(
                        get("/api/ratings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /api/ratings/{id} - should return 200 when user has allowed role")
    void getRatingById_shouldReturn200WhenAuthorized() throws Exception {

        RatingResponseDto response = new RatingResponseDto(
                1L,
                1L,
                1L,
                1L,
                5,
                "Great food!"
        );

        when(ratingService.getRatingById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/ratings/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}