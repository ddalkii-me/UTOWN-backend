package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.NotificationRequestDto;
import com.utown.utownbackend.dto.NotificationResponseDto;
import com.utown.utownbackend.dto.UnreadNotificationCountDto;
import com.utown.utownbackend.entity.Notification;
import com.utown.utownbackend.entity.NotificationType;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.NotificationRepository;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(1L);
        notification = TestDataFactory.createNotification(
                10L, user, NotificationType.ORDER_STATUS_CHANGED, "Order Placed", "Your order was received", false
        );
    }

    @Nested
    @DisplayName("Create Notification Tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("createNotification - should succeed and set isRead to false")
        void createNotification_shouldSucceed() {
            NotificationRequestDto request = new NotificationRequestDto(
                    1L, NotificationType.ORDER_STATUS_CHANGED, "Order Placed", "Your order was received"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> {
                Notification n = i.getArgument(0);
                n.setId(10L);
                return n;
            });

            NotificationResponseDto response = notificationService.createNotification(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.type()).isEqualTo(NotificationType.ORDER_STATUS_CHANGED);
            assertThat(response.title()).isEqualTo("Order Placed");
            assertThat(response.isRead()).isFalse();

            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("createNotification - should throw EntityNotFoundException when user does not exist")
        void createNotification_userNotFound_shouldThrow() {
            NotificationRequestDto request = new NotificationRequestDto(
                    99L, NotificationType.SYSTEM, "Hello", "World"
            );

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.createNotification(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("createNotification - should throw EntityNotFoundException when user is soft-deleted")
        void createNotification_userDeleted_shouldThrow() {
            user.setDeletedAt(LocalDateTime.now());
            NotificationRequestDto request = new NotificationRequestDto(
                    1L, NotificationType.SYSTEM, "Hello", "World"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> notificationService.createNotification(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Notifications Tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("getNotificationsForUser - without isRead filter should return all notifications")
        void getNotificationsForUser_all_shouldReturnList() {
            when(userRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

            List<NotificationResponseDto> results = notificationService.getNotificationsForUser(1L, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getNotificationsForUser - with isRead filter should return filtered list")
        void getNotificationsForUser_withFilter_shouldReturnList() {
            when(userRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);
            when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(1L, false))
                    .thenReturn(List.of(notification));

            List<NotificationResponseDto> results = notificationService.getNotificationsForUser(1L, false);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).isRead()).isFalse();
        }

        @Test
        @DisplayName("getNotificationsForUser - should throw EntityNotFoundException if user does not exist")
        void getNotificationsForUser_userNotFound_shouldThrow() {
            when(userRepository.existsByIdAndDeletedAtIsNull(99L)).thenReturn(false);

            assertThatThrownBy(() -> notificationService.getNotificationsForUser(99L, null))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("getNotificationById - should return notification when found for user")
        void getNotificationById_shouldReturnDto() {
            when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

            NotificationResponseDto response = notificationService.getNotificationById(10L, 1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getNotificationById - should throw EntityNotFoundException when not found or belongs to another user")
        void getNotificationById_notFound_shouldThrow() {
            when(notificationRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getNotificationById(10L, 2L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Notification not found");
        }

        @Test
        @DisplayName("getUnreadCount - should return unread badge count")
        void getUnreadCount_shouldReturnCount() {
            when(userRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

            UnreadNotificationCountDto countDto = notificationService.getUnreadCount(1L);

            assertThat(countDto).isNotNull();
            assertThat(countDto.count()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Read & Delete Tests")
    class ReadAndDeleteTests {

        @Test
        @DisplayName("markAsRead - should mark single notification as read")
        void markAsRead_shouldSucceed() {
            when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            NotificationResponseDto response = notificationService.markAsRead(10L, 1L);

            assertThat(response.isRead()).isTrue();
            assertThat(notification.getIsRead()).isTrue();
        }

        @Test
        @DisplayName("markAsRead - should throw EntityNotFoundException if not found")
        void markAsRead_notFound_shouldThrow() {
            when(notificationRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(99L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("markAllAsRead - should perform bulk update for user")
        void markAllAsRead_shouldSucceed() {
            when(userRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);

            notificationService.markAllAsRead(1L);

            verify(notificationRepository).markAllAsReadByUserId(1L);
        }

        @Test
        @DisplayName("deleteNotification - should delete notification when user owns it")
        void deleteNotification_shouldSucceed() {
            when(notificationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(notification));

            notificationService.deleteNotification(10L, 1L);

            verify(notificationRepository).delete(notification);
        }

        @Test
        @DisplayName("deleteNotification - should throw EntityNotFoundException if not found or belongs to another user")
        void deleteNotification_notFound_shouldThrow() {
            when(notificationRepository.findByIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.deleteNotification(10L, 2L))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(notificationRepository, never()).delete(any());
        }
    }
}
