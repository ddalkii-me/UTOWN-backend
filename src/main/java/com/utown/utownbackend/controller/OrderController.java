package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request
    ) {
        OrderResponseDto response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(
            @Valid @RequestBody CheckoutRequestDto request
    ) {
        OrderResponseDto response = orderService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<OrderStatus> status
    ) {
        return ResponseEntity.ok(orderService.getOrders(restaurantId, userId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<OrderResponseDto> acceptOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderAcceptRequestDto request
    ) {
        return ResponseEntity.ok(orderService.acceptOrder(id, request));
    }

    @PatchMapping("/{id}/prepare")
    public ResponseEntity<OrderResponseDto> startPreparation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.startPreparation(id));
    }

    @PatchMapping("/{id}/ready")
    public ResponseEntity<OrderResponseDto> markReadyForPickup(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.markReadyForPickup(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<OrderResponseDto> completeOrder(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(orderService.completeOrder(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderCancelRequestDto request
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(id, request));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<OrderResponseDto> declineOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderDeclineRequestDto request
    ) {
        return ResponseEntity.ok(orderService.declineOrder(id, request));
    }
}
