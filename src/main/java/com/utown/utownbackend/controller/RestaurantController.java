package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.service.MenuService;
import com.utown.utownbackend.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurant", description = "API for managing restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final MenuService menuService;

    @Operation(summary = "Create restaurant")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponseDto> createRestaurant(
            @Valid @RequestBody RestaurantRequestDto request) {
        log.debug("Entering createRestaurant method");

        RestaurantResponseDto response =
                restaurantService.createRestaurant(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Get restaurant by id")
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<RestaurantResponseDto>> getAllRestaurants() {
        log.debug("Entering getAllRestaurants method");

        List<RestaurantResponseDto> restaurants =
                restaurantService.getAllRestaurants();

        return ResponseEntity.ok(restaurants);
    }

    @Operation(summary = "Get restaurant by id")
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RestaurantResponseDto> getRestaurantById(
            @PathVariable Long id) {
        log.debug("Entering getRestaurantById method with id={}", id);

        RestaurantResponseDto response =
                restaurantService.getRestaurantById(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get restaurant menu")
    @GetMapping("/{id}/menu")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RestaurantMenuResponseDto> getRestaurantMenu(
            @PathVariable Long id) {
        log.debug("Entering getRestaurantMenu method with id={}", id);

        RestaurantMenuResponseDto response =
                menuService.getRestaurantMenu(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/working-hours")
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<WorkingHoursDto>> getWorkingHoursById(
            @PathVariable Long id
    ) {
        log.debug("Entering getWorkingHoursById method with id={}", id);
        List<WorkingHoursDto> response = restaurantService.getWorkingHours(id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update working hour for day")
    @PutMapping("/{id}/working-hours/{dayOfWeek}")
    @PreAuthorize(
            "hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #id)"
    )
    public ResponseEntity<Void> updateWorkingHourForDay(
            @PathVariable Long id,
            @PathVariable DayOfWeek dayOfWeek,
            @Valid @RequestBody WorkingHoursDto request
    ) {
        log.debug("Entering updateWorkingHourForDay method with id={}, dayOfWeek={}", id, dayOfWeek);
        restaurantService.updateWorkingHourForDay(id, dayOfWeek, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update restaurant")
    @PutMapping("/{id}")
    @PreAuthorize("@restaurantSecurity.isOwner(authentication, #id)")
    public ResponseEntity<RestaurantResponseDto> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantOwnerUpdateRequestDto request) {
        log.debug("Entering updateRestaurant method with id={}", id);

        RestaurantResponseDto response =
                restaurantService.updateRestaurant(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update restaurant as admin")
    @PutMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestaurantResponseDto> updateRestaurantAsAdmin(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantAdminUpdateRequestDto request) {
        log.debug("Entering updateRestaurantAsAdmin method with id={}", id);

        RestaurantResponseDto response =
                restaurantService.updateRestaurantAsAdmin(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete restaurant")
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #id)"
    )
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable Long id) {
        log.debug("Entering deleteRestaurant method with id={}", id);

        restaurantService.deleteRestaurant(id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/logo")
    @PreAuthorize("hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #id)")
    public ResponseEntity<RestaurantResponseDto> uploadRestaurantLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        RestaurantResponseDto response = restaurantService.uploadRestaurantLogo(id, file);
        return ResponseEntity.ok(response);
    }
}