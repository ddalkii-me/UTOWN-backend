package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.security.CustomUserDetails;
import com.utown.utownbackend.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and (#request.userId() == null or #request.userId() == authentication.principal.id))")
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request
    ) {
        Long resolvedUserId = request.userId();
        if (resolvedUserId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                resolvedUserId = userDetails.getId();
            }
        }
        OrderRequestDto effectiveRequest = (request.userId() == null && resolvedUserId != null)
                ? new OrderRequestDto(resolvedUserId, request.restaurantId(), request.addressId(), request.deliveryNote(), request.paymentMethod(), request.items())
                : request;
        OrderResponseDto response = orderService.createOrder(effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and (#request.userId() == null or #request.userId() == authentication.principal.id))")
    public ResponseEntity<OrderResponseDto> checkout(
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        Long resolvedUserId = request.userId();
        if (resolvedUserId == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                resolvedUserId = userDetails.getId();
            }
        }
        CheckoutRequestDto effectiveRequest = (request.userId() == null && resolvedUserId != null)
                ? new CheckoutRequestDto(resolvedUserId, request.addressId(), request.paymentMethod(), request.deliveryNote())
                : request;
        OrderResponseDto response = orderService.checkout(effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<OrderStatus> status,
            Authentication authentication
    ) {
        List<OrderResponseDto> orders =
                orderService.getOrders(restaurantId, userId, status, authentication);

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
        hasRole('ADMIN')
        or @orderSecurity.isCustomer(authentication, #id)
        or @orderSecurity.isRestaurantOwner(authentication, #id)
        """)
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isRestaurantOwner(authentication, #id)")
    public ResponseEntity<OrderResponseDto> acceptOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderAcceptRequestDto request
    ) {
        return ResponseEntity.ok(orderService.acceptOrder(id, request));
    }

    @PatchMapping("/{id}/prepare")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isRestaurantOwner(authentication, #id)")
    public ResponseEntity<OrderResponseDto> startPreparation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.startPreparation(id));
    }

    @PatchMapping("/{id}/ready")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isRestaurantOwner(authentication, #id)")
    public ResponseEntity<OrderResponseDto> markReadyForPickup(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.markReadyForPickup(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isRestaurantOwner(authentication, #id)")
    public ResponseEntity<OrderResponseDto> completeOrder(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isCustomer(authentication, #id)")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderCancelRequestDto request
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(id, request));
    }

    @PatchMapping("/{id}/decline")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isRestaurantOwner(authentication, #id)")
    public ResponseEntity<OrderResponseDto> declineOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderDeclineRequestDto request
    ) {
        return ResponseEntity.ok(orderService.declineOrder(id, request));
    }
}
