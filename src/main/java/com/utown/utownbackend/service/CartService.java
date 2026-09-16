package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AddToCartRequestDto;
import com.utown.utownbackend.dto.CartResponseDto;
import com.utown.utownbackend.dto.UpdateCartItemRequestDto;

public interface CartService {

    CartResponseDto getCart(Long userId);

    CartResponseDto addItemToCart(Long userId, AddToCartRequestDto request, boolean clearExisting);

    CartResponseDto updateCartItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequestDto request);

    CartResponseDto removeCartItem(Long userId, Long cartItemId);

    void clearCart(Long userId);
}
