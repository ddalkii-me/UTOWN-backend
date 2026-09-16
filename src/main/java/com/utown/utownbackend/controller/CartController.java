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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@RequestParam Long userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponseDto> addItemToCart(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean clearExisting,
            @Valid @RequestBody AddToCartRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, request, clearExisting));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> updateCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponseDto> removeCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeCartItem(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
