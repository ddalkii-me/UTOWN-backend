package com.utown.utownbackend.controller;

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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @Test
    @DisplayName("GET /api/cart - returns 200 OK with cart response")
    void getCart_returns200() throws Exception {
        CartResponseDto response = new CartResponseDto(
                1L, 10L, 5L, "Test Restaurant", CartStatus.ACTIVE,
                new BigDecimal("25.00"), 2, List.of(), null, null
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
    @DisplayName("GET /api/users/{userId}/cart - returns 200 OK with cart response")
    void getUserCart_returns200() throws Exception {
        CartResponseDto response = CartResponseDto.empty(10L);

        when(cartService.getCart(10L)).thenReturn(response);

        mockMvc.perform(get("/api/users/10/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("POST /api/cart/items - returns 201 Created on valid addition")
    void addItemToCart_returns201() throws Exception {
        AddToCartRequestDto request = new AddToCartRequestDto(5L, 100L, 2, List.of(1L, 2L));
        CartResponseDto response = new CartResponseDto(
                1L, 10L, 5L, "Test Restaurant", CartStatus.ACTIVE,
                new BigDecimal("30.00"), 2, List.of(), null, null
        );

        when(cartService.addItemToCart(eq(10L), any(AddToCartRequestDto.class), eq(false)))
                .thenReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    @DisplayName("POST /api/cart/items - returns 400 Bad Request on invalid quantity")
    void addItemToCart_invalidQuantity_returns400() throws Exception {
        AddToCartRequestDto request = new AddToCartRequestDto(5L, 100L, 0, null);

        mockMvc.perform(post("/api/cart/items")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/{userId}/cart/items - returns 201 Created on user route")
    void addUserCartItem_returns201() throws Exception {
        AddToCartRequestDto request = new AddToCartRequestDto(5L, 100L, 1, null);
        CartResponseDto response = CartResponseDto.empty(10L);

        when(cartService.addItemToCart(eq(10L), any(AddToCartRequestDto.class), eq(false)))
                .thenReturn(response);

        mockMvc.perform(post("/api/users/10/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("PUT /api/cart/items/{itemId} - returns 200 OK on update")
    void updateCartItem_returns200() throws Exception {
        UpdateCartItemRequestDto request = new UpdateCartItemRequestDto(3);
        CartResponseDto response = new CartResponseDto(
                1L, 10L, 5L, "Test Restaurant", CartStatus.ACTIVE,
                new BigDecimal("45.00"), 3, List.of(), null, null
        );

        when(cartService.updateCartItemQuantity(eq(10L), eq(50L), any(UpdateCartItemRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/cart/items/50")
                        .param("userId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} - returns 200 OK on removal")
    void removeCartItem_returns200() throws Exception {
        CartResponseDto response = CartResponseDto.empty(10L);

        when(cartService.removeCartItem(10L, 50L)).thenReturn(response);

        mockMvc.perform(delete("/api/cart/items/50")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("DELETE /api/cart - returns 204 No Content on clear")
    void clearCart_returns204() throws Exception {
        mockMvc.perform(delete("/api/cart")
                        .param("userId", "10"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(10L);
    }

    @Test
    @DisplayName("DELETE /api/users/{userId}/cart - returns 204 No Content on user route clear")
    void clearUserCart_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/10/cart"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(10L);
    }
}
