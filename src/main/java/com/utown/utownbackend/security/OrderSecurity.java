package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurity {

    private final OrderRepository orderRepository;

    public boolean isCustomer(Authentication authentication, Long orderId) {
        if (authentication == null || orderId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        return orderRepository.findById(orderId)
                .map(order -> order.getUser().getId().equals(userId))
                .orElse(false);
    }

    public boolean isRestaurantOwner(Authentication authentication, Long orderId) {
        if (authentication == null || orderId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        return orderRepository.findById(orderId)
                .map(order ->
                        order.getRestaurant()
                                .getOwner()
                                .getId()
                                .equals(userId)
                )
                .orElse(false);
    }
}
