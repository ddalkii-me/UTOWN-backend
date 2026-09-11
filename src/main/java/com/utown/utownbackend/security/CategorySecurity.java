package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("categorySecurity")
@RequiredArgsConstructor
public class CategorySecurity {

    private final CategoryRepository categoryRepository;

    public boolean isOwner(Authentication authentication, Long categoryId) {
        if (authentication == null || categoryId == null) {
            return false;
        }

        Long userId = ((CustomUserDetails) authentication.getPrincipal()).getId();

        return categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .map(category ->
                        category.getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
}