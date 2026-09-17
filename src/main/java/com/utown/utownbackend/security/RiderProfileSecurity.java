package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.repository.RiderProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("riderProfileSecurity")
@RequiredArgsConstructor
public class RiderProfileSecurity {

    private final RiderProfileRepository riderProfileRepository;

    public boolean isOwner(Authentication authentication, Long riderProfileId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            return false;
        }

        if (userDetails.getRole() != UserRole.RIDER) {
            return false;
        }

        if (riderProfileId == null || userDetails.getId() == null) {
            return false;
        }

        return riderProfileRepository.findByIdAndUserDeletedAtIsNull(riderProfileId)
                .map(riderProfile -> riderProfile.getUser() != null
                        && riderProfile.getStatus() == RiderStatus.ACTIVE
                        && userDetails.getId().equals(riderProfile.getUser().getId()))
                .orElse(false);
    }

}