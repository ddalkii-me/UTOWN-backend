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

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return orderRepository.findById(orderId)
                .map(order -> order.getUser() != null && userDetails.getId().equals(order.getUser().getId()))
                .orElse(false);
    }

    public boolean isRestaurantOwner(Authentication authentication, Long orderId) {
        if (authentication == null || orderId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return false;
        }

        return orderRepository.findById(orderId)
                .map(order ->
                        order.getRestaurant() != null
                                && order.getRestaurant().getOwner() != null
                                && userDetails.getId().equals(order.getRestaurant().getOwner().getId())
                )
                .orElse(false);
    }
}
