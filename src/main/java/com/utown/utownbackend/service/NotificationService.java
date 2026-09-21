package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;

import java.util.List;

public interface NotificationService {

    NotificationResponseDto createNotification(NotificationRequestDto request);

    List<NotificationResponseDto> getNotificationsForUser(Long userId, Boolean isRead);

    NotificationResponseDto getNotificationById(Long id, Long userId);

    NotificationResponseDto getNotificationById(Long id);

    UnreadNotificationCountDto getUnreadCount(Long userId);

    NotificationResponseDto markAsRead(Long id, Long userId);

    NotificationResponseDto markAsRead(Long id);

    void markAllAsRead(Long userId);

    void deleteNotification(Long id, Long userId);

    void deleteNotification(Long id);
}
