package com.utown.utownbackend.integration;

import com.utown.utownbackend.dto.RestaurantAdminUpdateRequestDto;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.WorkingHoursDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class RestaurantApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private RestaurantTypeRepository restaurantTypeRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private User owner;
    private City city;
    private RestaurantType type;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("e2e_owner@example.com");
        owner.setPhone("01099999999");
        owner.setName("E2E Owner");
        owner.setPassword("password");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        owner = userRepository.save(owner);

        city = new City();
        city.setName("Seoul E2E");
        city = cityRepository.save(city);

        type = new RestaurantType();
        type.setName("Italian E2E");
        type.setDescription("Pasta & Pizza");
        type = restaurantTypeRepository.save(type);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Complete restaurant lifecycle: POST -> GET -> PUT -> WORKING-HOURS -> DELETE")
    void completeRestaurantLifecycle() throws Exception {
        // 1. POST - Create restaurant
        RestaurantRequestDto createRequest = new RestaurantRequestDto(
                owner.getId(), type.getId(), city.getId(),
                "Luigi's Trattoria", "Authentic Italian", "100 Gangnam-daero",
                "02-555-1234", "http://example.com/luigi.png",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0),
                BigDecimal.valueOf(15000), RestaurantStatus.OPEN
        );

        String createResponseJson = mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Luigi's Trattoria"))
                .andExpect(jsonPath("$.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.cityId").value(city.getId()))
                .andReturn().getResponse().getContentAsString();

        Long restaurantId = objectMapper.readTree(createResponseJson).get("id").asLong();
        assertThat(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).isPresent();
        Restaurant savedRestaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId).get();
        assertThat(savedRestaurant.getCreatedAt()).isNotNull();
        assertThat(savedRestaurant.getUpdatedAt()).isNotNull();

        // 2. GET - Retrieve by ID
        mockMvc.perform(get("/api/restaurants/{id}", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(restaurantId))
                .andExpect(jsonPath("$.name").value("Luigi's Trattoria"));

        // 3. PUT - Update restaurant as ADMIN
        RestaurantAdminUpdateRequestDto updateRequest = new RestaurantAdminUpdateRequestDto(
                owner.getId(), type.getId(), city.getId(),
                "Luigi's Grand Trattoria", "Renovated Italian", "100 Gangnam-daero",
                "02-555-9999", "http://example.com/luigi2.png",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0),
                BigDecimal.valueOf(20000), RestaurantStatus.OPEN
        );

        mockMvc.perform(put("/api/restaurants/{id}/admin", restaurantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luigi's Grand Trattoria"))
                .andExpect(jsonPath("$.minimumOrderAmount").value(20000));

        // 4. PUT - Update Working Hours
        WorkingHoursDto mondayHours = new WorkingHoursDto(
                DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false
        );

        mockMvc.perform(put("/api/restaurants/{id}/working-hours/{dayOfWeek}", restaurantId, "MONDAY")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mondayHours)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/{id}/working-hours", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].openTime").value("10:00:00"));

        // 5. DELETE - Soft delete restaurant
        mockMvc.perform(delete("/api/restaurants/{id}", restaurantId))
                .andExpect(status().isNoContent());

        // Verify soft-deleted in DB and subsequent GET returns 404
        assertThat(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).isEmpty();
        mockMvc.perform(get("/api/restaurants/{id}", restaurantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/restaurants - returns 404 when owner does not exist")
    void createRestaurant_nonExistentOwner_returns404() throws Exception {
        RestaurantRequestDto invalidRequest = new RestaurantRequestDto(
                99999L, type.getId(), city.getId(),
                "Ghost Diner", "Desc", "Addr", "02-000-0000", null,
                null, null, BigDecimal.valueOf(10000), RestaurantStatus.OPEN
        );

        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Owner not found"));
    }
}
