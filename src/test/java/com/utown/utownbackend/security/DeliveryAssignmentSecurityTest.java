package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.DeliveryAssignment;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.DeliveryAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentSecurityTest {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private DeliveryAssignment assignment;

    @Mock
    private RiderProfile riderProfile;

    @Mock
    private Restaurant restaurant;

    @Mock
    private User riderUser;

    @Mock
    private User restaurantOwner;

    private DeliveryAssignmentSecurity security;

    @BeforeEach
    void setUp() {
        security = new DeliveryAssignmentSecurity(deliveryAssignmentRepository);
    }

    private Authentication authentication(Long userId) {
        CustomUserDetails principal = mock(CustomUserDetails.class);

        when(principal.getId()).thenReturn(userId);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null
        );
    }

    @Test
    void isRiderOwner_shouldReturnTrueForAssignmentOwner() {
        when(deliveryAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(assignment.getRider()).thenReturn(riderProfile);
        when(riderProfile.getUser()).thenReturn(riderUser);
        when(riderUser.getId()).thenReturn(5L);

        Authentication authentication = authentication(5L);

        assertTrue(security.isRiderOwner(1L, authentication));
    }

    @Test
    void isRiderOwner_shouldReturnFalseForDifferentRider() {
        when(deliveryAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(assignment.getRider()).thenReturn(riderProfile);
        when(riderProfile.getUser()).thenReturn(riderUser);
        when(riderUser.getId()).thenReturn(5L);

        Authentication authentication = authentication(10L);

        assertFalse(security.isRiderOwner(1L, authentication));
    }

    @Test
    void isRestaurantOwner_shouldReturnTrueForRestaurantOwner() {
        when(deliveryAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(assignment.getOrder())
                .thenReturn(mock(com.utown.utownbackend.entity.Order.class));

        when(assignment.getOrder().getRestaurant())
                .thenReturn(restaurant);

        when(restaurant.getOwner())
                .thenReturn(restaurantOwner);

        when(restaurantOwner.getId())
                .thenReturn(2L);

        Authentication authentication = authentication(2L);

        assertTrue(security.isRestaurantOwner(1L, authentication));
    }

    @Test
    void isRestaurantOwner_shouldReturnFalseForDifferentRestaurantOwner() {
        when(deliveryAssignmentRepository.findById(1L))
                .thenReturn(Optional.of(assignment));

        when(assignment.getOrder())
                .thenReturn(mock(com.utown.utownbackend.entity.Order.class));

        when(assignment.getOrder().getRestaurant())
                .thenReturn(restaurant);

        when(restaurant.getOwner())
                .thenReturn(restaurantOwner);

        when(restaurantOwner.getId())
                .thenReturn(2L);

        Authentication authentication = authentication(10L);

        assertFalse(security.isRestaurantOwner(1L, authentication));
    }

    @Test
    void isRiderOwner_shouldReturnFalseForNullAuthentication() {
        assertFalse(security.isRiderOwner(1L, null));
    }

    @Test
    void isRiderOwner_shouldReturnFalseForNullAssignmentId() {
        Authentication authentication = mock(Authentication.class);

        assertFalse(security.isRiderOwner(null, authentication));
    }

    @Test
    void isRestaurantOwner_shouldReturnFalseForNullAuthentication() {
        assertFalse(security.isRestaurantOwner(1L, null));
    }

    @Test
    void isRestaurantOwner_shouldReturnFalseForNullAssignmentId() {
        Authentication authentication = mock(Authentication.class);

        assertFalse(security.isRestaurantOwner(null, authentication));
    }
}