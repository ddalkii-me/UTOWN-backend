package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;
import com.utown.utownbackend.service.DishOptionGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/dish-option-groups")
@RequiredArgsConstructor
@Tag(name = "Dish Option Group", description = "API for managing dish option groups")
public class DishOptionGroupController {

    private final DishOptionGroupService dishOptionGroupService;

    @Operation(summary = "Create dish option group")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @dishOptionGroupSecurity.isOwnerOfDish(authentication, #request.dishId())")
    public ResponseEntity<DishOptionGroupResponseDto> createDishOptionGroup(
            @Valid @RequestBody DishOptionGroupRequestDto request
    ) {
        log.debug("Entering createDishOptionGroup method");
        DishOptionGroupResponseDto response = dishOptionGroupService.createDishOptionGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DishOptionGroupResponseDto>> getAllDishOptionGroups() {
        log.debug("Entering getAllDishOptionGroups method");
        return ResponseEntity.ok(dishOptionGroupService.getAllDishOptionGroups());
    }

    @Operation(summary = "Get dish option group by id")
    @GetMapping("/dish/{dishId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DishOptionGroupResponseDto>> getDishOptionGroupsByDishId(
            @PathVariable Long dishId
    ) {
        log.debug("Entering getDishOptionGroupsByDishId method with dishId={}", dishId);
        return ResponseEntity.ok(dishOptionGroupService.getDishOptionGroupsByDishId(dishId));
    }

    @Operation(summary = "Get dish option group by id")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DishOptionGroupResponseDto> getDishOptionGroupById(
            @PathVariable Long id
    ) {
        log.debug("Entering getDishOptionGroupById method with id={}", id);
        return ResponseEntity.ok(dishOptionGroupService.getDishOptionGroupById(id));
    }

    @Operation(summary = "Update dish option group")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishOptionGroupSecurity.isOwner(authentication, #id)")
    public ResponseEntity<DishOptionGroupResponseDto> updateDishOptionGroup(
            @PathVariable Long id,
            @Valid @RequestBody DishOptionGroupRequestDto request
    ) {
        log.debug("Entering updateDishOptionGroup method with id={}", id);
        return ResponseEntity.ok(dishOptionGroupService.updateDishOptionGroup(id, request));
    }

    @Operation(summary = "Delete dish option group")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishOptionGroupSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteDishOptionGroup(
            @PathVariable Long id
    ) {
        log.debug("Entering deleteDishOptionGroup method with id={}", id);
        dishOptionGroupService.deleteDishOptionGroup(id);
        return ResponseEntity.noContent().build();
    }
}
