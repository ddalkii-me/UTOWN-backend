package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<RestaurantResponseDto> createRestaurant(
            @Valid @RequestBody RestaurantRequestDto request) {

        RestaurantResponseDto response =
                restaurantService.createRestaurant(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping
    public ResponseEntity<List<RestaurantResponseDto>> getAllRestaurants() {

        List<RestaurantResponseDto> restaurants =
                restaurantService.getAllRestaurants();

        return ResponseEntity.ok(restaurants);
    }
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponseDto> getRestaurantById(
            @PathVariable Long id) {

        RestaurantResponseDto response =
                restaurantService.getRestaurantById(id);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponseDto> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequestDto request) {

        RestaurantResponseDto response =
                restaurantService.updateRestaurant(id, request);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable Long id) {

        restaurantService.deleteRestaurant(id);

        return ResponseEntity.noContent().build();
    }
}