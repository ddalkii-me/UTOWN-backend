package com.utown.utownbackend.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {

    public boolean isSelf(Authentication authentication, Long userId) {
        if (authentication == null || userId == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails principal) {
            return userId.equals(principal.getId());
        }
        return false;
    }

    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        if (authentication == null || userId == null) {
            return false;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        return isSelf(authentication, userId);
    }
}
