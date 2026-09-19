package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import com.utown.utownbackend.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-assignments")
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @PostMapping
    @PreAuthorize("""
        hasRole('ADMIN') or
        @orderSecurity.isRestaurantOwner(authentication, #request.orderId())
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> createAssignment(
            @Valid @RequestBody DeliveryAssignmentRequestDto request
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @deliveryAssignmentSecurity.isRestaurantOwner(#id, authentication) or
        @deliveryAssignmentSecurity.isRiderOwner(#id, authentication)
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> getAssignmentById(
            @PathVariable Long id
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.getAssignmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryAssignmentResponseDto>> getAssignments(
            @RequestParam(required = false) Long riderId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) DeliveryAssignmentStatus status
    ) {
        List<DeliveryAssignmentResponseDto> list = deliveryAssignmentService.getAssignments(riderId, orderId, status);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @deliveryAssignmentSecurity.isRiderOwner(#id, authentication)
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> acceptAssignment(
            @PathVariable Long id
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.acceptAssignment(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pickup")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @deliveryAssignmentSecurity.isRiderOwner(#id, authentication)
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> pickupDelivery(
            @PathVariable Long id
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.pickupDelivery(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deliver")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @deliveryAssignmentSecurity.isRiderOwner(#id, authentication)
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> completeDelivery(
            @PathVariable Long id
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.completeDelivery(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @deliveryAssignmentSecurity.isRiderOwner(#id, authentication)
        """)
    public ResponseEntity<DeliveryAssignmentResponseDto> cancelAssignment(
            @PathVariable Long id
    ) {
        DeliveryAssignmentResponseDto response = deliveryAssignmentService.cancelAssignment(id);
        return ResponseEntity.ok(response);
    }
}
