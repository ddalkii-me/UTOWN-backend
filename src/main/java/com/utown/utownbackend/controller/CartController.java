package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.AddToCartRequestDto;
import com.utown.utownbackend.dto.CartResponseDto;
import com.utown.utownbackend.dto.UpdateCartItemRequestDto;
import com.utown.utownbackend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "API for managing carts")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get cart")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(#userId, authentication)")
    public ResponseEntity<CartResponseDto> getCart(@RequestParam Long userId) {
        log.debug("Entering getCart method with userId={}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(summary = "Add item to cart")
    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(#userId, authentication)")
    public ResponseEntity<CartResponseDto> addItemToCart(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean clearExisting,
            @Valid @RequestBody AddToCartRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItemToCart(userId, request, clearExisting));
    }

    @Operation(summary = "Update cart item")
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(#userId, authentication)")
    public ResponseEntity<CartResponseDto> updateCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequestDto request) {
        log.debug("Entering updateCartItem method with userId={}, itemId={}", userId, itemId);
        return ResponseEntity.ok(cartService.updateCartItemQuantity(userId, itemId, request));
    }

    @Operation(summary = "Remove cart item")
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(#userId, authentication)")
    public ResponseEntity<CartResponseDto> removeCartItem(
            @RequestParam Long userId,
            @PathVariable Long itemId) {
        log.debug("Entering removeCartItem method with userId={}, itemId={}", userId, itemId);
        return ResponseEntity.ok(cartService.removeCartItem(userId, itemId));
    }

    @Operation(summary = "Clear cart")
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN') or @cartSecurity.isOwner(#userId, authentication)")
    public ResponseEntity<Void> clearCart(@RequestParam Long userId) {
        log.debug("Entering clearCart method with userId={}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
