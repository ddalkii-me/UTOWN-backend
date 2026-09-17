package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.AddressRequestDto;
import com.utown.utownbackend.dto.AddressResponseDto;
import com.utown.utownbackend.dto.AddressUpdateRequestDto;
import com.utown.utownbackend.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #request.userId() == authentication.principal.id)")
    public ResponseEntity<AddressResponseDto> createAddress(
            @Valid @RequestBody AddressRequestDto request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(addressService.createAddress(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AddressResponseDto>> getAllAddresses() {

        return ResponseEntity.ok(
                addressService.getAllAddresses()
        );
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CUSTOMER') and #userId == authentication.principal.id)")
    public ResponseEntity<List<AddressResponseDto>> getAddressesByUser(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                addressService.getAddressesByUser(userId)
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @addressSecurity.isOwner(authentication, #id)")
    public ResponseEntity<AddressResponseDto> getAddressById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                addressService.getAddressById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @addressSecurity.isOwner(authentication, #id)")
    public ResponseEntity<AddressResponseDto> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressUpdateRequestDto request) {

        return ResponseEntity.ok(
                addressService.updateAddress(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @addressSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id) {

        addressService.deleteAddress(id);

        return ResponseEntity.noContent().build();
    }
}