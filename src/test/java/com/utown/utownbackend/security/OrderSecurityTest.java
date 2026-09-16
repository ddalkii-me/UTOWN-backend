package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.Order;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.repository.OrderRepository;
import com.utown.utownbackend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSecurityTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderSecurity orderSecurity;

    private User customerUser;
    private User ownerUser;
    private Authentication customerAuth;
    private Authentication ownerAuth;
    private Order order;

    @BeforeEach
    void setUp() {
        customerUser = TestDataFactory.createUser(10L);
        customerUser.setRole(UserRole.CUSTOMER);
        CustomUserDetails customerDetails = new CustomUserDetails(customerUser);
        customerAuth = new UsernamePasswordAuthenticationToken(customerDetails, null, customerDetails.getAuthorities());

        ownerUser = TestDataFactory.createUser(20L);
        ownerUser.setRole(UserRole.RESTAURANT_OWNER);
        CustomUserDetails ownerDetails = new CustomUserDetails(ownerUser);
        ownerAuth = new UsernamePasswordAuthenticationToken(ownerDetails, null, ownerDetails.getAuthorities());

        Restaurant restaurant = new Restaurant();
        restaurant.setId(100L);
        restaurant.setOwner(ownerUser);

        order = new Order();
        order.setId(500L);
        order.setUser(customerUser);
        order.setRestaurant(restaurant);
    }

    @Test
    @DisplayName("isCustomer - returns true when authenticated customer owns the order")
    void isCustomer_matchingCustomer_returnsTrue() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThat(orderSecurity.isCustomer(customerAuth, 500L)).isTrue();
    }

    @Test
    @DisplayName("isCustomer - returns false when authenticated user is not the order owner")
    void isCustomer_differentCustomer_returnsFalse() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThat(orderSecurity.isCustomer(ownerAuth, 500L)).isFalse();
    }

    @Test
    @DisplayName("isCustomer - returns false when order does not exist")
    void isCustomer_orderNotFound_returnsFalse() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(orderSecurity.isCustomer(customerAuth, 999L)).isFalse();
    }

    @Test
    @DisplayName("isCustomer - returns false when authentication or orderId is null")
    void isCustomer_nullParams_returnsFalse() {
        assertThat(orderSecurity.isCustomer(null, 500L)).isFalse();
        assertThat(orderSecurity.isCustomer(customerAuth, null)).isFalse();
    }

    @Test
    @DisplayName("isCustomer - returns false when principal is not CustomUserDetails")
    void isCustomer_stringPrincipal_returnsFalse() {
        Authentication stringAuth = new UsernamePasswordAuthenticationToken("anonymousUser", null);

        assertThat(orderSecurity.isCustomer(stringAuth, 500L)).isFalse();
    }

    @Test
    @DisplayName("isRestaurantOwner - returns true when authenticated owner owns the restaurant")
    void isRestaurantOwner_matchingOwner_returnsTrue() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThat(orderSecurity.isRestaurantOwner(ownerAuth, 500L)).isTrue();
    }

    @Test
    @DisplayName("isRestaurantOwner - returns false when authenticated user is not the restaurant owner")
    void isRestaurantOwner_differentOwner_returnsFalse() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThat(orderSecurity.isRestaurantOwner(customerAuth, 500L)).isFalse();
    }

    @Test
    @DisplayName("isRestaurantOwner - returns false when restaurant has no owner")
    void isRestaurantOwner_noOwner_returnsFalse() {
        order.getRestaurant().setOwner(null);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(order));

        assertThat(orderSecurity.isRestaurantOwner(ownerAuth, 500L)).isFalse();
    }

    @Test
    @DisplayName("isRestaurantOwner - returns false when order does not exist")
    void isRestaurantOwner_orderNotFound_returnsFalse() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(orderSecurity.isRestaurantOwner(ownerAuth, 999L)).isFalse();
    }

    @Test
    @DisplayName("isRestaurantOwner - returns false when authentication or orderId is null")
    void isRestaurantOwner_nullParams_returnsFalse() {
        assertThat(orderSecurity.isRestaurantOwner(null, 500L)).isFalse();
        assertThat(orderSecurity.isRestaurantOwner(ownerAuth, null)).isFalse();
    }
}
