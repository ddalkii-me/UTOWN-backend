package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;
import com.utown.utownbackend.service.DishOptionGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dish-option-groups")
@RequiredArgsConstructor
public class DishOptionGroupController {

    private final DishOptionGroupService dishOptionGroupService;

    @PostMapping
    public ResponseEntity<DishOptionGroupResponseDto> createDishOptionGroup(
            @Valid @RequestBody DishOptionGroupRequestDto request
    ) {
        DishOptionGroupResponseDto response = dishOptionGroupService.createDishOptionGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DishOptionGroupResponseDto>> getAllDishOptionGroups() {
        return ResponseEntity.ok(dishOptionGroupService.getAllDishOptionGroups());
    }

    @GetMapping("/dish/{dishId}")
    public ResponseEntity<List<DishOptionGroupResponseDto>> getDishOptionGroupsByDishId(
            @PathVariable Long dishId
    ) {
        return ResponseEntity.ok(dishOptionGroupService.getDishOptionGroupsByDishId(dishId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DishOptionGroupResponseDto> getDishOptionGroupById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dishOptionGroupService.getDishOptionGroupById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DishOptionGroupResponseDto> updateDishOptionGroup(
            @PathVariable Long id,
            @Valid @RequestBody DishOptionGroupRequestDto request
    ) {
        return ResponseEntity.ok(dishOptionGroupService.updateDishOptionGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDishOptionGroup(
            @PathVariable Long id
    ) {
        dishOptionGroupService.deleteDishOptionGroup(id);
        return ResponseEntity.noContent().build();
    }
}
