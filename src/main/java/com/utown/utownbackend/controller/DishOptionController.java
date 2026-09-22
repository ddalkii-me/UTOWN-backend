package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.service.DishOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/dish-options")
@RequiredArgsConstructor
public class DishOptionController {

    private final DishOptionService dishOptionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @dishOptionSecurity.isOwnerOfOptionGroup(authentication, #request.optionGroupId())")
    public ResponseEntity<DishOptionResponseDto> createDishOption(
            @Valid @RequestBody DishOptionRequestDto request
    ) {
        log.debug("Entering createDishOption method");
        DishOptionResponseDto response = dishOptionService.createDishOption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DishOptionResponseDto>> getAllDishOptions() {
        log.debug("Entering getAllDishOptions method");
        return ResponseEntity.ok(dishOptionService.getAllDishOptions());
    }

    @GetMapping("/group/{optionGroupId}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<DishOptionResponseDto>> getDishOptionsByGroupId(
            @PathVariable Long optionGroupId
    ) {
        log.debug("Entering getDishOptionsByGroupId method with optionGroupId={}", optionGroupId);
        return ResponseEntity.ok(dishOptionService.getDishOptionsByGroupId(optionGroupId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<DishOptionResponseDto> getDishOptionById(
            @PathVariable Long id
    ) {
        log.debug("Entering getDishOptionById method with id={}", id);
        return ResponseEntity.ok(dishOptionService.getDishOptionById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishOptionSecurity.isOwner(authentication, #id)")
    public ResponseEntity<DishOptionResponseDto> updateDishOption(
            @PathVariable Long id,
            @Valid @RequestBody DishOptionRequestDto request
    ) {
        log.debug("Entering updateDishOption method with id={}", id);
        return ResponseEntity.ok(dishOptionService.updateDishOption(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dishOptionSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteDishOption(
            @PathVariable Long id
    ) {
        log.debug("Entering deleteDishOption method with id={}", id);
        dishOptionService.deleteDishOption(id);
        return ResponseEntity.noContent().build();
    }
}
