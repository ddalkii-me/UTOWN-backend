package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("dishOptionGroupSecurity")
@RequiredArgsConstructor
public class DishOptionGroupSecurity {

    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final DishRepository dishRepository;

    public boolean isOwnerOfDish(
            Authentication authentication,
            Long dishId
    ) {
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

    public boolean isOwner(
            Authentication authentication,
            Long optionGroupId
    ) {
        if (authentication == null || optionGroupId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        return dishOptionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)
                .map(group ->
                        group.getDish()
                                .getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
}