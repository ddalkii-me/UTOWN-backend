package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.service.DishService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @PostMapping
    public ResponseEntity<DishResponseDto> createDish(
            @Valid @RequestBody DishRequestDto request
    ) {
        DishResponseDto response = dishService.createDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DishResponseDto>> getDishes(
            @RequestParam(required = false) DishStatus status,
            @RequestParam(required = false, defaultValue = "false") boolean deleted
    ) {
        List<DishResponseDto> dishes = dishService.getDishes(status, deleted);
        return ResponseEntity.ok(dishes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishResponseDto> getDishById(
            @PathVariable Long id
    ) {
        DishResponseDto response = dishService.getDishById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishResponseDto> updateDishById(
            @Valid @RequestBody DishRequestDto request, @PathVariable Long id
    ) {
        DishResponseDto response = dishService.updateDish(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDishById(
            @PathVariable Long id
    ) {
        dishService.deleteDish(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restoreDishById(
            @PathVariable Long id
    ) {
        dishService.restoreDish(id);
        return ResponseEntity.noContent().build();
    }
}
