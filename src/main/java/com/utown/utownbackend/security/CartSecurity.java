package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CartSecurity {

    public boolean isOwner(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return userDetails.getRole() == UserRole.CUSTOMER
                && userDetails.getId().equals(userId);
    }
}