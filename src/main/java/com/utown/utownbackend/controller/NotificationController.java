package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/notifications")
    public ResponseEntity<NotificationResponseDto> createNotification(
            @Valid @RequestBody NotificationRequestDto request
    ) {
        NotificationResponseDto response = notificationService.createNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/notifications", "/users/{userIdPathVariable}/notifications"})
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @RequestParam(required = false) Long userId,
            @PathVariable(required = false) Long userIdPathVariable,
            @RequestParam(required = false) Boolean isRead
    ) {
        Long targetUserId = resolveUserId(userId, userIdPathVariable);
        List<NotificationResponseDto> list = notificationService.getNotificationsForUser(targetUserId, isRead);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/notifications/{id}")
    public ResponseEntity<NotificationResponseDto> getNotificationById(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        NotificationResponseDto response = notificationService.getNotificationById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/notifications/unread-count", "/users/{userIdPathVariable}/notifications/unread-count"})
    public ResponseEntity<UnreadNotificationCountDto> getUnreadCount(
            @RequestParam(required = false) Long userId,
            @PathVariable(required = false) Long userIdPathVariable
    ) {
        Long targetUserId = resolveUserId(userId, userIdPathVariable);
        UnreadNotificationCountDto response = notificationService.getUnreadCount(targetUserId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        NotificationResponseDto response = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping({"/notifications/read-all", "/users/{userIdPathVariable}/notifications/read-all"})
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam(required = false) Long userId,
            @PathVariable(required = false) Long userIdPathVariable
    ) {
        Long targetUserId = resolveUserId(userId, userIdPathVariable);
        notificationService.markAllAsRead(targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Long paramUserId, Long pathUserId) {
        if (pathUserId != null) {
            return pathUserId;
        }
        if (paramUserId != null) {
            return paramUserId;
        }
        throw new IllegalArgumentException("userId is required");
    }
}
