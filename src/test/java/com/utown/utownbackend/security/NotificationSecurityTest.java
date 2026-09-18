package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.Notification;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationSecurityTest {

    private NotificationRepository notificationRepository;
    private NotificationSecurity notificationSecurity;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        notificationSecurity = new NotificationSecurity(notificationRepository);
    }

    private Authentication createAuthentication(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    @Test
    @DisplayName("isOwner - should return true when notification belongs to authenticated user")
    void isOwner_owner_shouldReturnTrue() {

        Authentication authentication = createAuthentication(1L);

        when(notificationRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(mock(Notification.class)));

        boolean result =
                notificationSecurity.isOwner(authentication, 10L);

        assertTrue(result);

        verify(notificationRepository)
                .findByIdAndUserId(10L, 1L);
    }

    @Test
    @DisplayName("isOwner - should return false when notification does not belong to authenticated user")
    void isOwner_notOwner_shouldReturnFalse() {

        Authentication authentication = createAuthentication(1L);

        when(notificationRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        boolean result =
                notificationSecurity.isOwner(authentication, 10L);

        assertFalse(result);

        verify(notificationRepository)
                .findByIdAndUserId(10L, 1L);
    }

    @Test
    @DisplayName("isOwner - should return false when authentication is null")
    void isOwner_nullAuthentication_shouldReturnFalse() {

        boolean result =
                notificationSecurity.isOwner(null, 10L);

        assertFalse(result);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("isOwner - should return false when authentication is not authenticated")
    void isOwner_notAuthenticated_shouldReturnFalse() {

        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result =
                notificationSecurity.isOwner(authentication, 10L);

        assertFalse(result);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("isOwner - should return false when principal is not CustomUserDetails")
    void isOwner_wrongPrincipal_shouldReturnFalse() {

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "username",
                        null
                );

        boolean result =
                notificationSecurity.isOwner(authentication, 10L);

        assertFalse(result);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("isOwner - should return false when user ID is null")
    void isOwner_nullUserId_shouldReturnFalse() {

        Authentication authentication = createAuthentication(null);

        boolean result =
                notificationSecurity.isOwner(authentication, 10L);

        assertFalse(result);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("isOwner - should return false when notification ID is null")
    void isOwner_nullNotificationId_shouldReturnFalse() {

        Authentication authentication = createAuthentication(1L);

        boolean result =
                notificationSecurity.isOwner(authentication, null);

        assertFalse(result);

        verifyNoInteractions(notificationRepository);
    }
}