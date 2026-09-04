package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.FavoriteRestaurantResponseDto;
import com.utown.utownbackend.service.FavoriteRestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteRestaurantController {

    private final FavoriteRestaurantService favoriteRestaurantService;

    @PostMapping("/{restaurantId}")
    public ResponseEntity<FavoriteRestaurantResponseDto> addFavorite(
            @PathVariable Long userId,
            @PathVariable Long restaurantId) {

        FavoriteRestaurantResponseDto response =
                favoriteRestaurantService.addFavorite(userId, restaurantId);

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FavoriteRestaurantResponseDto>> getFavorites(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                favoriteRestaurantService.getFavoritesByUser(userId)
        );
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable Long userId,
            @PathVariable Long restaurantId) {

        favoriteRestaurantService.removeFavorite(userId, restaurantId);

        return ResponseEntity.noContent().build();
    }
}