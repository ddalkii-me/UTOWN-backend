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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
        log.debug("Entering createRating method with orderId={}", orderId);
        RatingResponseDto response = ratingService.createRating(
                orderId,
                request
        );

        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/restaurants/{restaurantId}/ratings")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RatingResponseDto>> getRatingsByRestaurant(
            @PathVariable Long restaurantId
    ) {
        log.debug("Entering getRatingsByRestaurant method with restaurantId={}", restaurantId);
        return ResponseEntity.ok(
                ratingService.getRatingsByRestaurant(restaurantId)
        );
    }

    @GetMapping("/ratings/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER')")
    public ResponseEntity<RatingResponseDto> getRatingById(
            @PathVariable Long id
    ) {
        log.debug("Entering getRatingById method with id={}", id);
        return ResponseEntity.ok(
                ratingService.getRatingById(id)
        );
    }

    @DeleteMapping("/ratings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long id
    ) {
        log.debug("Entering deleteRating method with id={}", id);
        ratingService.deleteRating(id);

        return ResponseEntity.noContent().build();
    }
}