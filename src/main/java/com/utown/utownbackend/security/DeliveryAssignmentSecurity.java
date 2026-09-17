package com.utown.utownbackend.security;

import com.utown.utownbackend.repository.DeliveryAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("deliveryAssignmentSecurity")
@RequiredArgsConstructor
public class DeliveryAssignmentSecurity {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;

    public boolean isRiderOwner(Long assignmentId, Authentication authentication) {
        if (authentication == null || assignmentId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        if (userId == null) {
            return false;
        }

        return deliveryAssignmentRepository.findById(assignmentId)
                .map(assignment ->
                        assignment.getRider() != null
                                && assignment.getRider().getUser() != null
                                && userId.equals(assignment.getRider().getUser().getId())
                )
                .orElse(false);
    }

    public boolean isRestaurantOwner(Long assignmentId, Authentication authentication) {
        if (authentication == null || assignmentId == null) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        Long userId = principal.getId();

        if (userId == null) {
            return false;
        }

        return deliveryAssignmentRepository.findById(assignmentId)
                .map(assignment ->
                        assignment.getOrder() != null
                                && assignment.getOrder().getRestaurant() != null
                                && assignment.getOrder().getRestaurant().getOwner() != null
                                && userId.equals(
                                assignment.getOrder()
                                        .getRestaurant()
                                        .getOwner()
                                        .getId()
                        )
                )
                .orElse(false);
    }
}