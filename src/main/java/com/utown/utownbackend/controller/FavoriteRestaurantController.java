package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.FavoriteRestaurantResponseDto;
import com.utown.utownbackend.service.FavoriteRestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteRestaurantController {

    private final FavoriteRestaurantService favoriteRestaurantService;

    @PostMapping("/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #userId == authentication.principal.id)")
    public ResponseEntity<FavoriteRestaurantResponseDto> addFavorite(
            @PathVariable Long userId,
            @PathVariable Long restaurantId) {
        log.debug("Entering addFavorite method with userId={}, restaurantId={}", userId, restaurantId);

        FavoriteRestaurantResponseDto response =
                favoriteRestaurantService.addFavorite(userId, restaurantId);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #userId == authentication.principal.id)")
    public ResponseEntity<List<FavoriteRestaurantResponseDto>> getFavorites(
            @PathVariable Long userId) {
        log.debug("Entering getFavorites method with userId={}", userId);

        return ResponseEntity.ok(
                favoriteRestaurantService.getFavoritesByUser(userId)
        );
    }

    @DeleteMapping("/{restaurantId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #userId == authentication.principal.id)")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long userId,
            @PathVariable Long restaurantId) {
        log.debug("Entering removeFavorite method with userId={}, restaurantId={}", userId, restaurantId);

        favoriteRestaurantService.removeFavorite(userId, restaurantId);

        return ResponseEntity.noContent().build();
    }
}