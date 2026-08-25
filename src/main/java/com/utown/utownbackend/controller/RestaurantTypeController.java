package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.service.RestaurantTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant-types")
@RequiredArgsConstructor
public class RestaurantTypeController {

    private final RestaurantTypeService restaurantTypeService;

    @PostMapping
    public ResponseEntity<RestaurantTypeResponseDto> createRestaurantType(
            @RequestBody RestaurantTypeRequestDto request) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.createRestaurantType(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RestaurantTypeResponseDto>> getAllRestaurantTypes() {

        List<RestaurantTypeResponseDto> restaurantTypes =
                restaurantTypeService.getAllRestaurantTypes();

        return ResponseEntity.ok(restaurantTypes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantTypeResponseDto> getRestaurantTypeById(
            @PathVariable Long id) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.getRestaurantTypeById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantTypeResponseDto> updateRestaurantType(
            @PathVariable Long id,
            @RequestBody RestaurantTypeRequestDto request) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.updateRestaurantType(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurantType(
            @PathVariable Long id) {

        restaurantTypeService.deleteRestaurantType(id);

        return ResponseEntity.noContent().build();
    }
}