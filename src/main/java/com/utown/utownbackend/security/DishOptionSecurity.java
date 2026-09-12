package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.DishOption;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import com.utown.utownbackend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("dishOptionSecurity")
@RequiredArgsConstructor
public class DishOptionSecurity {

    private final DishOptionRepository dishOptionRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;

    public boolean isOwner(Authentication authentication, Long dishOptionId) {
        if (authentication == null || dishOptionId == null) {
            return false;
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return dishOptionRepository.findByIdAndDeletedAtIsNull(dishOptionId)
                .map(option ->
                        option.getOptionGroup()
                                .getDish()
                                .getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }

    public boolean isOwnerOfOptionGroup(
            Authentication authentication,
            Long optionGroupId
    ) {
        if (authentication == null || optionGroupId == null) {
            return false;
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

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