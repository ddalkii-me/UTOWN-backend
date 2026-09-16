package com.utown.utownbackend.controller;

import com.utown.utownbackend.security.CartSecurity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.AddToCartRequestDto;
import com.utown.utownbackend.dto.CartResponseDto;
import com.utown.utownbackend.dto.UpdateCartItemRequestDto;
import com.utown.utownbackend.entity.CartStatus;
import com.utown.utownbackend.service.CartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc
@Import(CartControllerTest.MethodSecurityTestConfig.class)
@WithMockUser(roles = "CUSTOMER")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Autowired
    private CartSecurity cartSecurity;


    @Test
    @DisplayName("GET /api/cart - customer can access own cart")
    void getCart_returns200() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        CartResponseDto response = new CartResponseDto(
                1L,
                10L,
                5L,
                "Test Restaurant",
                CartStatus.ACTIVE,
                new BigDecimal("25.00"),
                2,
                List.of(),
                null,
                null
        );

        when(cartService.getCart(10L)).thenReturn(response);

        mockMvc.perform(get("/api/cart")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.restaurantName").value("Test Restaurant"))
                .andExpect(jsonPath("$.totalAmount").value(25.00))
                .andExpect(jsonPath("$.totalItems").value(2));
    }


    @Test
    @DisplayName("GET /api/cart - customer cannot access another user's cart")
    void getCart_customerOtherUsersCart_returns403() throws Exception {

        when(cartSecurity.isOwner(eq(25L), any(Authentication.class)))
                .thenReturn(false);

        mockMvc.perform(get("/api/cart")
                        .param("userId", "25"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getCart(anyLong());
    }

    @Test
    @DisplayName("POST /api/cart/items - returns 201 Created on valid addition")
    void addItemToCart_returns201() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        AddToCartRequestDto request =
                new AddToCartRequestDto(
                        5L,
                        100L,
                        2,
                        List.of(1L, 2L)
                );

        CartResponseDto response = new CartResponseDto(
                1L,
                10L,
                5L,
                "Test Restaurant",
                CartStatus.ACTIVE,
                new BigDecimal("30.00"),
                2,
                List.of(),
                null,
                null
        );

        when(cartService.addItemToCart(
                eq(10L),
                any(AddToCartRequestDto.class),
                eq(false)
        )).thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .param("userId", "10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalItems").value(2));
    }


    @Test
    @DisplayName("POST /api/cart/items - returns 400 Bad Request on invalid quantity")
    void addItemToCart_invalidQuantity_returns400() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        AddToCartRequestDto request =
                new AddToCartRequestDto(
                        5L,
                        100L,
                        0,
                        null
                );

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .param("userId", "10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/cart/items - customer cannot modify another user's cart")
    void addItemToCart_customerOtherUsersCart_returns403() throws Exception {

        AddToCartRequestDto request =
                new AddToCartRequestDto(
                        5L,
                        100L,
                        2,
                        List.of(1L, 2L)
                );

        when(cartSecurity.isOwner(eq(25L), any(Authentication.class)))
                .thenReturn(false);

        mockMvc.perform(post("/api/cart/items")
                        .with(csrf())
                        .param("userId", "25")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).addItemToCart(
                anyLong(),
                any(AddToCartRequestDto.class),
                anyBoolean()
        );
    }


    @Test
    @DisplayName("PUT /api/cart/items/{itemId} - returns 200 OK on update")
    void updateCartItem_returns200() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        UpdateCartItemRequestDto request =
                new UpdateCartItemRequestDto(3);

        CartResponseDto response = new CartResponseDto(
                1L,
                10L,
                5L,
                "Test Restaurant",
                CartStatus.ACTIVE,
                new BigDecimal("45.00"),
                3,
                List.of(),
                null,
                null
        );

        when(cartService.updateCartItemQuantity(
                eq(10L),
                eq(50L),
                any(UpdateCartItemRequestDto.class)
        )).thenReturn(response);

        mockMvc.perform(put("/api/cart/items/50")
                        .with(csrf())
                        .param("userId", "10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }


    @Test
    @DisplayName("PUT /api/cart/items/{itemId} - returns 400 Bad Request on invalid quantity")
    void updateCartItem_invalidQuantity_returns400() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        UpdateCartItemRequestDto request =
                new UpdateCartItemRequestDto(0);

        mockMvc.perform(put("/api/cart/items/50")
                        .with(csrf())
                        .param("userId", "10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("PUT /api/cart/items/{itemId} - customer cannot modify another user's cart")
    void updateCartItem_customerOtherUsersCart_returns403() throws Exception {

        UpdateCartItemRequestDto request =
                new UpdateCartItemRequestDto(3);

        when(cartSecurity.isOwner(eq(25L), any(Authentication.class)))
                .thenReturn(false);

        mockMvc.perform(put("/api/cart/items/50")
                        .with(csrf())
                        .param("userId", "25")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).updateCartItemQuantity(
                anyLong(),
                anyLong(),
                any(UpdateCartItemRequestDto.class)
        );
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} - returns 200 OK on removal")
    void removeCartItem_returns200() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        CartResponseDto response =
                CartResponseDto.empty(10L);

        when(cartService.removeCartItem(10L, 50L))
                .thenReturn(response);

        mockMvc.perform(delete("/api/cart/items/50")
                        .with(csrf())
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }


    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} - customer cannot modify another user's cart")
    void removeCartItem_customerOtherUsersCart_returns403() throws Exception {

        when(cartSecurity.isOwner(eq(25L), any(Authentication.class)))
                .thenReturn(false);

        mockMvc.perform(delete("/api/cart/items/50")
                        .with(csrf())
                        .param("userId", "25"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).removeCartItem(
                anyLong(),
                anyLong()
        );
    }


    @Test
    @DisplayName("DELETE /api/cart - returns 204 No Content on clear")
    void clearCart_returns204() throws Exception {

        when(cartSecurity.isOwner(eq(10L), any(Authentication.class)))
                .thenReturn(true);

        mockMvc.perform(delete("/api/cart")
                        .with(csrf())
                        .param("userId", "10"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(10L);
    }


    @Test
    @DisplayName("DELETE /api/cart - customer cannot clear another user's cart")
    void clearCart_customerOtherUsersCart_returns403() throws Exception {

        when(cartSecurity.isOwner(eq(25L), any(Authentication.class)))
                .thenReturn(false);

        mockMvc.perform(delete("/api/cart")
                        .with(csrf())
                        .param("userId", "25"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).clearCart(anyLong());
    }


    @Test
    @DisplayName("GET /api/cart - admin can access another user's cart")
    @WithMockUser(roles = "ADMIN")
    void getCart_admin_canAccessAnotherUsersCart() throws Exception {

        CartResponseDto response = new CartResponseDto(
                1L,
                25L,
                5L,
                "Test Restaurant",
                CartStatus.ACTIVE,
                new BigDecimal("25.00"),
                2,
                List.of(),
                null,
                null
        );

        when(cartService.getCart(25L))
                .thenReturn(response);

        mockMvc.perform(get("/api/cart")
                        .param("userId", "25"))
                .andExpect(status().isOk());

        verify(cartService).getCart(25L);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
        @Bean
        @Primary
        CartSecurity cartSecurity() {
            return mock(CartSecurity.class);
        }
    }
}