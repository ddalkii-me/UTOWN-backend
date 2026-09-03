package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.service.DishOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dish-options")
@RequiredArgsConstructor
public class DishOptionController {

    private final DishOptionService dishOptionService;

    @PostMapping
    public ResponseEntity<DishOptionResponseDto> createDishOption(
            @Valid @RequestBody DishOptionRequestDto request
    ) {
        DishOptionResponseDto response = dishOptionService.createDishOption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DishOptionResponseDto>> getAllDishOptions() {
        return ResponseEntity.ok(dishOptionService.getAllDishOptions());
    }

    @GetMapping("/group/{optionGroupId}")
    public ResponseEntity<List<DishOptionResponseDto>> getDishOptionsByGroupId(
            @PathVariable Long optionGroupId
    ) {
        return ResponseEntity.ok(dishOptionService.getDishOptionsByGroupId(optionGroupId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishOptionResponseDto> getDishOptionById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dishOptionService.getDishOptionById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishOptionResponseDto> updateDishOption(
            @PathVariable Long id,
            @Valid @RequestBody DishOptionRequestDto request
    ) {
        return ResponseEntity.ok(dishOptionService.updateDishOption(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDishOption(
            @PathVariable Long id
    ) {
        dishOptionService.deleteDishOption(id);
        return ResponseEntity.noContent().build();
    }
}
