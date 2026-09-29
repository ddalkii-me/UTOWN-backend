package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.ChangePasswordRequestDto;
import com.utown.utownbackend.dto.UserProfileResponseDto;
import com.utown.utownbackend.dto.UserProfileUpdateRequestDto;
import com.utown.utownbackend.dto.UserStatusUpdateRequestDto;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDto> getCurrentUserProfile(Principal principal) {
        log.debug("Entering getCurrentUserProfile method with principal={}", principal);
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return ResponseEntity.ok(userService.getCurrentUserProfile(principal.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponseDto> updateCurrentUserProfile(
            Principal principal,
            @Valid @RequestBody UserProfileUpdateRequestDto request) {
        log.debug("Entering updateCurrentUserProfile method with principal={}", principal);
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return ResponseEntity.ok(userService.updateCurrentUserProfile(principal.getName(), request));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        log.debug("Entering changePassword method with principal={}", principal);
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteCurrentUser(Principal principal) {
        log.debug("Entering deleteCurrentUser method with principal={}", principal);
        if (principal == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        userService.deleteCurrentUser(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(authentication, #id)")
    public ResponseEntity<UserProfileResponseDto> getUserById(@PathVariable Long id) {
        log.debug("Entering getUserById method with id={}", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponseDto>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status) {
        return ResponseEntity.ok(userService.getUsers(role, status));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponseDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequestDto request) {
        log.debug("Entering updateUserStatus method with id={}", id);
        return ResponseEntity.ok(userService.updateUserStatus(id, request));
    }
}
