package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.service.DishService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/dishes")
@Tag(name = "Dish", description = "API for managing dishs")
public class DishController {

    final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @Operation(summary = "Create dish")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #request.restaurantId())")
    public ResponseEntity<DishResponseDto> createDish(
            @Valid @RequestBody DishRequestDto request
    ) {
        log.debug("Entering createDish method");
        DishResponseDto response = dishService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get dish by id")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DishResponseDto>> getDishes(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) DishStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean deleted
    ) {
        List<DishResponseDto> dishes = dishService.getDishes(restaurantId, categoryId, status, deleted);
        return ResponseEntity.ok(dishes);
    }

    @Operation(summary = "Get dish by id")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DishResponseDto> getDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering getDishById method with id={}", id);
        DishResponseDto response = dishService.getDishById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update dish by id")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwner(authentication, #id)")
    public ResponseEntity<DishResponseDto> updateDishById(
            @Valid @RequestBody DishRequestDto request, @PathVariable Long id
    ) {
        log.debug("Entering updateDishById method with id={}", id);
        DishResponseDto response = dishService.updateDish(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete dish by id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering deleteDishById method with id={}", id);
        dishService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore dish by id")
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwnerOfDeletedDish(authentication, #id)")
    public ResponseEntity<Void> restoreDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering restoreDishById method with id={}", id);
        dishService.restoreDish(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwner(authentication, #id)")
    public ResponseEntity<DishResponseDto> uploadDishImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        DishResponseDto response = dishService.uploadDishImage(id, file);
        return ResponseEntity.ok(response);
    }
}
