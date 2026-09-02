package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DeliveryAreaRequestDto;
import com.utown.utownbackend.dto.DeliveryAreaResponseDto;
import com.utown.utownbackend.service.DeliveryAreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-areas")
@RequiredArgsConstructor
public class DeliveryAreaController {

    private final DeliveryAreaService deliveryAreaService;

    @PostMapping
    public ResponseEntity<DeliveryAreaResponseDto> createDeliveryArea(
            @Valid @RequestBody DeliveryAreaRequestDto request) {

        DeliveryAreaResponseDto response =
                deliveryAreaService.createDeliveryArea(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeliveryAreaResponseDto>> getAllDeliveryAreas() {

        List<DeliveryAreaResponseDto> response =
                deliveryAreaService.getAllDeliveryAreas();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeliveryAreaResponseDto> getDeliveryAreaById(
            @PathVariable Long id) {

        DeliveryAreaResponseDto response =
                deliveryAreaService.getDeliveryAreaById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<DeliveryAreaResponseDto>> getDeliveryAreasByCity(
            @PathVariable Long cityId) {

        List<DeliveryAreaResponseDto> response =
                deliveryAreaService.getDeliveryAreasByCity(cityId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeliveryAreaResponseDto> updateDeliveryArea(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAreaRequestDto request) {

        DeliveryAreaResponseDto response =
                deliveryAreaService.updateDeliveryArea(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeliveryArea(
            @PathVariable Long id) {

        deliveryAreaService.deleteDeliveryArea(id);

        return ResponseEntity.noContent().build();
    }
}