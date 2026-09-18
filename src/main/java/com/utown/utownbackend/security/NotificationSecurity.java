package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("notificationSecurity")
@RequiredArgsConstructor
public class NotificationSecurity {

    private final NotificationRepository notificationRepository;

    public boolean isOwner(Authentication authentication, Long notificationId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        Long userId = userDetails.getId();

        if (userId == null || notificationId == null) {
            return false;
        }

        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .isPresent();
    }
}