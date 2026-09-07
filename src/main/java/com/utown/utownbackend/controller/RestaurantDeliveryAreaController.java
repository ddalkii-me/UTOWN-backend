package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantDeliveryAreaResponseDto;
import com.utown.utownbackend.service.RestaurantDeliveryAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantDeliveryAreaController {

    private final RestaurantDeliveryAreaService restaurantDeliveryAreaService;

    @PostMapping("/{restaurantId}/delivery-areas/{deliveryAreaId}")
    public ResponseEntity<RestaurantDeliveryAreaResponseDto> addDeliveryArea(
            @PathVariable Long restaurantId,
            @PathVariable Long deliveryAreaId) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        restaurantDeliveryAreaService
                                .addDeliveryArea(restaurantId, deliveryAreaId)
                );
    }

    @GetMapping("/{restaurantId}/delivery-areas")
    public ResponseEntity<List<RestaurantDeliveryAreaResponseDto>>
    getDeliveryAreasByRestaurant(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(
                restaurantDeliveryAreaService
                        .getDeliveryAreasByRestaurant(restaurantId)
        );
    }

    @DeleteMapping("/{restaurantId}/delivery-areas/{deliveryAreaId}")
    public ResponseEntity<Void> removeDeliveryArea(
            @PathVariable Long restaurantId,
            @PathVariable Long deliveryAreaId) {

        restaurantDeliveryAreaService
                .removeDeliveryArea(restaurantId, deliveryAreaId);

        return ResponseEntity.noContent().build();
    }
}