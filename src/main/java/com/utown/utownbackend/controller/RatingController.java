package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RatingRequestDto;
import com.utown.utownbackend.dto.RatingResponseDto;
import com.utown.utownbackend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/users/{userId}/restaurants/{restaurantId}/orders/{orderId}/rating")
    public ResponseEntity<RatingResponseDto> createRating(
            @PathVariable Long userId,
            @PathVariable Long restaurantId,
            @PathVariable Long orderId,
            @Valid @RequestBody RatingRequestDto request
    ) {
        RatingResponseDto response = ratingService.createRating(
                userId,
                restaurantId,
                orderId,
                request
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/restaurants/{restaurantId}/ratings")
    public ResponseEntity<List<RatingResponseDto>> getRatingsByRestaurant(
            @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(
                ratingService.getRatingsByRestaurant(restaurantId)
        );
    }

    @GetMapping("/ratings/{id}")
    public ResponseEntity<RatingResponseDto> getRatingById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ratingService.getRatingById(id)
        );
    }

    @DeleteMapping("/ratings/{id}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long id
    ) {
        ratingService.deleteRating(id);

        return ResponseEntity.noContent().build();
    }
}