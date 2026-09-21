package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.entity.Notification;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.NotificationRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public NotificationResponseDto createNotification(NotificationRequestDto request) {
        log.info("Creating notification for user ID: {}, type: {}", request.userId(), request.type());

        User user = userRepository.findById(request.userId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(request.type());
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification ID: {} for user ID: {}", saved.getId(), user.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForUser(Long userId, Boolean isRead) {
        validateUserExists(userId);

        List<Notification> list = (isRead != null)
                ? notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, isRead)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return list.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    // Normal user — only their own notification
    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(Long id, Long userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));
    }

    // Admin — any notification
    @Override
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountDto getUnreadCount(Long userId) {
        validateUserExists(userId);
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new UnreadNotificationCountDto(count);
    }

    @Override
    public NotificationResponseDto markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification ID: {} as read for user ID: {}", id, userId);

        return mapToResponseDto(saved);
    }

    @Override
    public NotificationResponseDto markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        log.info("Marked notification ID: {} as read by admin", id);

        return mapToResponseDto(saved);
    }

    @Override
    public void markAllAsRead(Long userId) {
        validateUserExists(userId);
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user ID: {}", count, userId);
    }

    @Override
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

        notificationRepository.delete(notification);
        log.info("Deleted notification ID: {} for user ID: {}", id, userId);
    }

    @Override
    public void deleteNotification(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

        notificationRepository.delete(notification);
        log.info("Deleted notification ID: {} by admin", id);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }
    }

    private NotificationResponseDto mapToResponseDto(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getUser() != null ? n.getUser().getId() : null,
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getIsRead(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
