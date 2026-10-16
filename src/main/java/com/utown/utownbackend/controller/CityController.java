package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Tag(name = "City", description = "API for managing citys")
public class CityController {

    private final CityService cityService;

    @Operation(summary = "Create city")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityResponseDto> createCity(
            @Valid @RequestBody CityRequestDto request) {
        log.debug("Entering createCity method");

        CityResponseDto response =
                cityService.createCity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get city by id")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CityResponseDto>> getAllCities() {
        log.debug("Entering getAllCities method");

        List<CityResponseDto> cities =
                cityService.getAllCities();

        return ResponseEntity.ok(cities);
    }

    @Operation(summary = "Get city by id")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CityResponseDto> getCityById(
            @PathVariable Long id) {
        log.debug("Entering getCityById method with id={}", id);

        CityResponseDto response =
                cityService.getCityById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update city")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityResponseDto> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody CityRequestDto request) {
        log.debug("Entering updateCity method with id={}", id);

        CityResponseDto response =
                cityService.updateCity(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete city")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCity(
            @PathVariable Long id) {
        log.debug("Entering deleteCity method with id={}", id);

        cityService.deleteCity(id);

        return ResponseEntity.noContent().build();
    }
}