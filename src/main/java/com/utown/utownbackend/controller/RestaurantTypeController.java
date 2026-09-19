package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.service.RestaurantTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurant-types")
@RequiredArgsConstructor
public class RestaurantTypeController {

    private final RestaurantTypeService restaurantTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTypeResponseDto> createRestaurantType(
            @Valid @RequestBody RestaurantTypeRequestDto request) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.createRestaurantType(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RestaurantTypeResponseDto>> getAllRestaurantTypes() {

        List<RestaurantTypeResponseDto> restaurantTypes =
                restaurantTypeService.getAllRestaurantTypes();

        return ResponseEntity.ok(restaurantTypes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RestaurantTypeResponseDto> getRestaurantTypeById(
            @PathVariable Long id) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.getRestaurantTypeById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantTypeResponseDto> updateRestaurantType(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantTypeRequestDto request) {

        RestaurantTypeResponseDto response =
                restaurantTypeService.updateRestaurantType(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRestaurantType(
            @PathVariable Long id) {

        restaurantTypeService.deleteRestaurantType(id);

        return ResponseEntity.noContent().build();
    }
}