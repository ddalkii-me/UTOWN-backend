package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.service.RiderProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/riders")
@RequiredArgsConstructor
public class RiderProfileController {

    private final RiderProfileService riderProfileService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RiderProfileResponseDto> createRiderProfile(
            @Valid @RequestBody RiderProfileRequestDto request
    ) {
        RiderProfileResponseDto response = riderProfileService.createRiderProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RiderProfileResponseDto>> getAllRiderProfiles(
            @RequestParam(required = false) RiderStatus status,
            @RequestParam(required = false) Boolean availability
    ) {
        List<RiderProfileResponseDto> list = riderProfileService.getAllRiderProfiles(status, availability);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @riderProfileSecurity.isOwner(authentication, #id)")
    public ResponseEntity<RiderProfileResponseDto> getRiderProfileById(
            @PathVariable Long id
    ) {
        RiderProfileResponseDto response = riderProfileService.getRiderProfileById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("""
        hasRole('ADMIN') or
        @riderProfileSecurity.isOwnerByUserId(authentication, #userId)
        """)
    public ResponseEntity<RiderProfileResponseDto> getRiderProfileByUserId(
            @PathVariable Long userId
    ) {
        RiderProfileResponseDto response = riderProfileService.getRiderProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @riderProfileSecurity.isOwner(authentication, #id)")
    public ResponseEntity<RiderProfileResponseDto> updateRiderProfile(
            @PathVariable Long id,
            @Valid @RequestBody RiderProfileUpdateRequestDto request
    ) {
        RiderProfileResponseDto response = riderProfileService.updateRiderProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN') or @riderProfileSecurity.isOwner(authentication, #id)")
    public ResponseEntity<RiderProfileResponseDto> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody RiderAvailabilityUpdateRequestDto request
    ) {
        RiderProfileResponseDto response = riderProfileService.updateAvailability(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RiderProfileResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RiderStatusUpdateRequestDto request
    ) {
        RiderProfileResponseDto response = riderProfileService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRiderProfile(
            @PathVariable Long id
    ) {
        riderProfileService.deleteRiderProfile(id);
        return ResponseEntity.noContent().build();
    }
}
