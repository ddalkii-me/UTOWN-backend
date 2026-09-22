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

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dishes")
public class DishController {

    final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #request.restaurantId())")
    public ResponseEntity<DishResponseDto> createDish(
            @Valid @RequestBody DishRequestDto request
    ) {
        log.debug("Entering createDish method");
        DishResponseDto response = dishService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

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

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DishResponseDto> getDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering getDishById method with id={}", id);
        DishResponseDto response = dishService.getDishById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwner(authentication, #id)")
    public ResponseEntity<DishResponseDto> updateDishById(
            @Valid @RequestBody DishRequestDto request, @PathVariable Long id
    ) {
        log.debug("Entering updateDishById method with id={}", id);
        DishResponseDto response = dishService.updateDish(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering deleteDishById method with id={}", id);
        dishService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN') or @dishSecurity.isOwnerOfDeletedDish(authentication, #id)")
    public ResponseEntity<Void> restoreDishById(
            @PathVariable Long id
    ) {
        log.debug("Entering restoreDishById method with id={}", id);
        dishService.restoreDish(id);
        return ResponseEntity.noContent().build();
    }
}
