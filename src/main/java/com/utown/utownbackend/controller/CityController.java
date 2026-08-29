package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @PostMapping
    public ResponseEntity<CityResponseDto> createCity(
            @Valid @RequestBody CityRequestDto request) {

        CityResponseDto response =
                cityService.createCity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CityResponseDto>> getAllCities() {

        List<CityResponseDto> cities =
                cityService.getAllCities();

        return ResponseEntity.ok(cities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponseDto> getCityById(
            @PathVariable Long id) {

        CityResponseDto response =
                cityService.getCityById(id);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityResponseDto> updateCity(
            @PathVariable Long id,
            @Valid @RequestBody CityRequestDto request) {

        CityResponseDto response =
                cityService.updateCity(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCity(
            @PathVariable Long id) {

        cityService.deleteCity(id);

        return ResponseEntity.noContent().build();
    }
}