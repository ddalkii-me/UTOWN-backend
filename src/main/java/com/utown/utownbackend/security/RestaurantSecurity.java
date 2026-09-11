package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("restaurantSecurity")
@RequiredArgsConstructor
public class RestaurantSecurity {

    private final RestaurantRepository restaurantRepository;

    public boolean isOwner(Authentication authentication, Long restaurantId) {
        if (authentication == null || restaurantId == null) {
            return false;
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .map(restaurant ->
                        restaurant.getOwner().getId().equals(userId)
                )
                .orElse(false);
    }
}