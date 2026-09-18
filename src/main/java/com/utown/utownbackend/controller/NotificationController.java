package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationRequestDto request
    ) {
        NotificationResponseDto response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id)
            """)
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean isRead
    ) {
        List<NotificationResponseDto> list = notificationService.getNotificationsForUser(userId, isRead);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id
            and @notificationSecurity.isOwner(authentication, #id))
            """)
    public ResponseEntity<NotificationResponseDto> getNotificationById(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        NotificationResponseDto response = notificationService.getNotificationById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id)
            """)
    public ResponseEntity<UnreadNotificationCountDto> getUnreadCount(
            @RequestParam Long userId
    ) {
        UnreadNotificationCountDto response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id
            and @notificationSecurity.isOwner(authentication, #id))
            """)
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        NotificationResponseDto response = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id)
            """)
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam Long userId
    ) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("""
            hasRole('ADMIN') or
            ((hasRole('CUSTOMER') or hasRole('RESTAURANT_OWNER') or hasRole('RIDER'))
            and #userId == authentication.principal.id
            and @notificationSecurity.isOwner(authentication, #id))
            """)
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}
