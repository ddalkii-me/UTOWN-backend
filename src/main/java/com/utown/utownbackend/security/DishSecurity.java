package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("dishSecurity")
@RequiredArgsConstructor
public class DishSecurity {

    private final DishRepository dishRepository;

    public boolean isOwner(Authentication authentication, Long dishId) {
        if (authentication == null || dishId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        return dishRepository.findByIdAndDeletedAtIsNull(dishId)
                .map(dish ->
                        dish.getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
    public boolean isOwnerOfDeletedDish(
            Authentication authentication,
            Long dishId) {

        if (authentication == null || dishId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        return dishRepository.findByIdAndDeletedAtIsNotNull(dishId)
                .map(dish ->
                        dish.getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
}