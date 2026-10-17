package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DeliveryAreaRequestDto;
import com.utown.utownbackend.dto.DeliveryAreaResponseDto;
import com.utown.utownbackend.service.DeliveryAreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/delivery-areas")
@RequiredArgsConstructor
@Tag(name = "Delivery Area", description = "API for managing delivery areas")
public class DeliveryAreaController {

    private final DeliveryAreaService deliveryAreaService;

    @Operation(summary = "Create delivery area")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryAreaResponseDto> createDeliveryArea(
            @Valid @RequestBody DeliveryAreaRequestDto request) {
        log.debug("Entering createDeliveryArea method");

        DeliveryAreaResponseDto response =
                deliveryAreaService.createDeliveryArea(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get delivery area by id")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DeliveryAreaResponseDto>> getAllDeliveryAreas() {
        log.debug("Entering getAllDeliveryAreas method");

        List<DeliveryAreaResponseDto> response =
                deliveryAreaService.getAllDeliveryAreas();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get delivery area by id")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DeliveryAreaResponseDto> getDeliveryAreaById(
            @PathVariable Long id) {
        log.debug("Entering getDeliveryAreaById method with id={}", id);

        DeliveryAreaResponseDto response =
                deliveryAreaService.getDeliveryAreaById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update delivery area")
    @GetMapping("/city/{cityId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DeliveryAreaResponseDto>> getDeliveryAreasByCity(
            @PathVariable Long cityId) {
        log.debug("Entering getDeliveryAreasByCity method with cityId={}", cityId);

        List<DeliveryAreaResponseDto> response =
                deliveryAreaService.getDeliveryAreasByCity(cityId);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update delivery area")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryAreaResponseDto> updateDeliveryArea(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAreaRequestDto request) {
        log.debug("Entering updateDeliveryArea method with id={}", id);

        DeliveryAreaResponseDto response =
                deliveryAreaService.updateDeliveryArea(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete delivery area")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDeliveryArea(
            @PathVariable Long id) {
        log.debug("Entering deleteDeliveryArea method with id={}", id);

        deliveryAreaService.deleteDeliveryArea(id);

        return ResponseEntity.noContent().build();
    }
}