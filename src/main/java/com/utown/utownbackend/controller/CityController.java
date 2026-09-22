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

@Slf4j
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CityResponseDto> createCity(
            @Valid @RequestBody CityRequestDto request) {
        log.debug("Entering createCity method");

        CityResponseDto response =
                cityService.createCity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CityResponseDto>> getAllCities() {
        log.debug("Entering getAllCities method");

        List<CityResponseDto> cities =
                cityService.getAllCities();

        return ResponseEntity.ok(cities);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CityResponseDto> getCityById(
            @PathVariable Long id) {
        log.debug("Entering getCityById method with id={}", id);

        CityResponseDto response =
                cityService.getCityById(id);

        return ResponseEntity.ok(response);
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCity(
            @PathVariable Long id) {
        log.debug("Entering deleteCity method with id={}", id);

        cityService.deleteCity(id);

        return ResponseEntity.noContent().build();
    }
}