package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.AddToCartRequestDto;
import com.utown.utownbackend.dto.CartResponseDto;
import com.utown.utownbackend.dto.UpdateCartItemRequestDto;
import com.utown.utownbackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // --- /api/cart Endpoints ---

    @GetMapping("/api/cart")
    public ResponseEntity<CartResponseDto> getCart(@RequestParam Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/api/cart/items")
    public ResponseEntity<CartResponseDto> addItemToCart(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean clearExisting,
            @Valid @RequestBody AddToCartRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, request, clearExisting));
    }

    @PutMapping("/api/cart/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/api/cart/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeCartItem(userId, itemId));
    }

    @DeleteMapping("/api/cart")
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    // --- /api/users/{userId}/cart Endpoints (RESTful user resource route) ---

    @GetMapping("/api/users/{userId}/cart")
    public ResponseEntity<CartResponseDto> getUserCart(@PathVariable Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/api/users/{userId}/cart/items")
    public ResponseEntity<CartResponseDto> addUserCartItem(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean clearExisting,
            @Valid @RequestBody AddToCartRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, request, clearExisting));
    }

    @PutMapping("/api/users/{userId}/cart/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateUserCartItem(
            @PathVariable Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/api/users/{userId}/cart/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeUserCartItem(
            @PathVariable Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeCartItem(userId, itemId));
    }

    @DeleteMapping("/api/users/{userId}/cart")
    public ResponseEntity<Void> clearUserCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
