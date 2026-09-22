package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantDeliveryAreaResponseDto;
import com.utown.utownbackend.service.RestaurantDeliveryAreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantDeliveryAreaController {

    private final RestaurantDeliveryAreaService restaurantDeliveryAreaService;

    @PostMapping("/{restaurantId}/delivery-areas/{deliveryAreaId}")
    @PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #restaurantId)")
    public ResponseEntity<RestaurantDeliveryAreaResponseDto> addDeliveryArea(
            @PathVariable Long restaurantId,
            @PathVariable Long deliveryAreaId) {
        log.debug("Entering addDeliveryArea method with restaurantId={}, deliveryAreaId={}", restaurantId, deliveryAreaId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        restaurantDeliveryAreaService
                                .addDeliveryArea(restaurantId, deliveryAreaId)
                );
    }

    @GetMapping("/{restaurantId}/delivery-areas")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RestaurantDeliveryAreaResponseDto>>
    getDeliveryAreasByRestaurant(
            @PathVariable Long restaurantId) {
        log.debug("Entering getDeliveryAreasByRestaurant method with restaurantId={}", restaurantId);

        return ResponseEntity.ok(
                restaurantDeliveryAreaService
                        .getDeliveryAreasByRestaurant(restaurantId)
        );
    }

    @DeleteMapping("/{restaurantId}/delivery-areas/{deliveryAreaId}")
    @PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #restaurantId)")
    public ResponseEntity<Void> removeDeliveryArea(
            @PathVariable Long restaurantId,
            @PathVariable Long deliveryAreaId) {
        log.debug("Entering removeDeliveryArea method with restaurantId={}, deliveryAreaId={}", restaurantId, deliveryAreaId);

        restaurantDeliveryAreaService
                .removeDeliveryArea(restaurantId, deliveryAreaId);

        return ResponseEntity.noContent().build();
    }
}