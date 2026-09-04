package com.utown.utownbackend.controller;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.AddressRequestDto;
import com.utown.utownbackend.dto.AddressResponseDto;
import com.utown.utownbackend.service.AddressService;
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
@WebMvcTest(AddressController.class)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    private AddressRequestDto requestDto;
    private AddressResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = TestDataFactory.createAddressRequestDto(1L, 1L, 1L);
        responseDto = TestDataFactory.createAddressResponseDto(1L, 1L, 1L, 1L);
    }

    @Test
    @DisplayName("POST /api/addresses - should return 201")
    void createAddress_shouldReturn201() throws Exception {
        when(addressService.createAddress(any(AddressRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.recipientName").value("Test Recipient"));
    }

    @Test
    @DisplayName("POST /api/addresses - should return 400 when userId is null")
    void createAddress_shouldReturn400WhenUserIdNull() throws Exception {
        AddressRequestDto invalidRequest = new AddressRequestDto(null, 1L, 1L, "Home", "John Doe", "123456", "Main St 1", "12345", BigDecimal.valueOf(10), BigDecimal.valueOf(20));

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/addresses - should return 404 when user or city or area not found")
    void createAddress_shouldReturn404WhenEntityNotFound() throws Exception {
        when(addressService.createAddress(any(AddressRequestDto.class)))
                .thenThrow(new EntityNotFoundException("City not found"));

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/addresses - should return 200")
    void getAllAddresses_shouldReturn200() throws Exception {
        when(addressService.getAllAddresses()).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/addresses/user/{userId} - should return 200")
    void getAddressesByUser_shouldReturn200() throws Exception {
        when(addressService.getAddressesByUser(1L)).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/addresses/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/addresses/{id} - should return 200")
    void getAddressById_shouldReturn200() throws Exception {
        when(addressService.getAddressById(1L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/addresses/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/addresses/{id} - should return 404 when not found")
    void getAddressById_shouldReturn404WhenNotFound() throws Exception {
        when(addressService.getAddressById(99L))
                .thenThrow(new EntityNotFoundException("Address not found"));

        mockMvc.perform(get("/api/addresses/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/addresses/{id} - should return 200")
    void updateAddress_shouldReturn200() throws Exception {
        when(addressService.updateAddress(eq(1L), any(AddressRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/addresses/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /api/addresses/{id} - should return 404 when not found")
    void updateAddress_shouldReturn404WhenNotFound() throws Exception {
        when(addressService.updateAddress(eq(99L), any(AddressRequestDto.class)))
                .thenThrow(new EntityNotFoundException("Address not found"));

        mockMvc.perform(put("/api/addresses/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("PUT /api/addresses/{id} - should return 400 when invalid payload")
    void updateAddress_shouldReturn400WhenInvalid() throws Exception {
        AddressRequestDto invalidRequest = new AddressRequestDto(null, 1L, 1L, "Home", "", "123456", "", "12345", BigDecimal.valueOf(10), BigDecimal.valueOf(20));

        mockMvc.perform(put("/api/addresses/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/addresses/{id} - should return 204")
    void deleteAddress_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/addresses/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(addressService).deleteAddress(1L);
    }

    @Test
    @DisplayName("DELETE /api/addresses/{id} - should return 404 when not found")
    void deleteAddress_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new EntityNotFoundException("Address not found"))
                .when(addressService).deleteAddress(99L);

        mockMvc.perform(delete("/api/addresses/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
