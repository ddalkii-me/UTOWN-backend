package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RatingRequestDto;
import com.utown.utownbackend.dto.RatingResponseDto;
import com.utown.utownbackend.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/orders/{orderId}/ratings")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isCustomer(authentication, #orderId)")
    public ResponseEntity<RatingResponseDto> createRating(
            @PathVariable Long orderId,
            @Valid @RequestBody RatingRequestDto request
    ) {
        RatingResponseDto response = ratingService.createRating(
                orderId,
                request
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/restaurants/{restaurantId}/ratings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<List<RatingResponseDto>> getRatingsByRestaurant(
            @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(
                ratingService.getRatingsByRestaurant(restaurantId)
        );
    }

    @GetMapping("/ratings/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RatingResponseDto> getRatingById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ratingService.getRatingById(id)
        );
    }

    @DeleteMapping("/ratings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long id
    ) {
        ratingService.deleteRating(id);

        return ResponseEntity.noContent().build();
    }
}