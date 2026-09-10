package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AddToCartRequestDto;
import com.utown.utownbackend.dto.CartResponseDto;
import com.utown.utownbackend.dto.UpdateCartItemRequestDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartItemOptionRepository cartItemOptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private DishOptionRepository dishOptionRepository;

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private User activeUser;
    private Restaurant restaurant;
    private Dish dish;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setStatus(UserStatus.ACTIVE);

        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Test Restaurant");
        restaurant.setStatus(RestaurantStatus.OPEN);

        dish = new Dish();
        dish.setId(100L);
        dish.setName("Spicy Noodle");
        dish.setPrice(new BigDecimal("12.00"));
        dish.setStatus(DishStatus.AVAILABLE);
        dish.setRestaurant(restaurant);
    }

    @Test
    @DisplayName("getCart - returns empty cart when user has no active cart")
    void getCart_noCart_returnsEmpty() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponseDto result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(0);
        assertThat(result.totalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("getCart - returns empty cart when cart has expired")
    void getCart_expiredCart_returnsEmpty() {
        Cart expiredCart = new Cart();
        expiredCart.setId(5L);
        expiredCart.setUser(activeUser);
        expiredCart.setStatus(CartStatus.EXPIRED);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(expiredCart));

        CartResponseDto result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(0);
    }

    @Test
    @DisplayName("getCart - throws DisabledException when user is inactive")
    void getCart_inactiveUser_throwsDisabled() {
        User inactive = new User();
        inactive.setId(2L);
        inactive.setStatus(UserStatus.SUSPENDED);

        when(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> cartService.getCart(2L))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("User is not active");
    }

    @Test
    @DisplayName("getCart - returns populated cart with items and options")
    void getCart_populatedCart_returnsDetails() {
        Cart cart = new Cart();
        cart.setId(5L);
        cart.setUser(activeUser);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setId(50L);
        item.setCart(cart);
        item.setDish(dish);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.00"));
        item.setSubtotal(new BigDecimal("24.00"));

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(5L)).thenReturn(List.of(item));
        when(cartItemOptionRepository.findAllByCartItemIdIn(List.of(50L))).thenReturn(List.of());

        CartResponseDto result = cartService.getCart(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.restaurantName()).isEqualTo("Test Restaurant");
        assertThat(result.totalItems()).isEqualTo(2);
        assertThat(result.totalAmount()).isEqualByComparingTo("24.00");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("addItemToCart - creates new cart and adds item successfully")
    void addItemToCart_newCart_success() {
        AddToCartRequestDto request = new AddToCartRequestDto(10L, 100L, 2, null);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        Cart savedCart = new Cart();
        savedCart.setId(1L);
        savedCart.setUser(activeUser);
        savedCart.setRestaurant(restaurant);
        savedCart.setStatus(CartStatus.ACTIVE);

        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(dishRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(dish));
        when(dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(100L)).thenReturn(List.of());
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of());

        CartItem savedItem = new CartItem();
        savedItem.setId(10L);
        savedItem.setDish(dish);
        savedItem.setQuantity(2);
        savedItem.setUnitPrice(new BigDecimal("12.00"));
        savedItem.setSubtotal(new BigDecimal("24.00"));

        when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

        CartResponseDto response = cartService.addItemToCart(1L, request, false);

        assertThat(response).isNotNull();
        verify(cartRepository, atLeastOnce()).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItemToCart - merges quantity when same dish with identical options is added")
    void addItemToCart_sameDishAndOptions_mergesQuantity() {
        AddToCartRequestDto request = new AddToCartRequestDto(10L, 100L, 1, null);

        Cart existingCart = new Cart();
        existingCart.setId(1L);
        existingCart.setUser(activeUser);
        existingCart.setRestaurant(restaurant);
        existingCart.setStatus(CartStatus.ACTIVE);

        CartItem existingItem = new CartItem();
        existingItem.setId(10L);
        existingItem.setDish(dish);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("12.00"));
        existingItem.setSubtotal(new BigDecimal("24.00"));

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(existingCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(existingCart);
        when(dishRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(dish));
        when(dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(100L)).thenReturn(List.of());
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of(existingItem));
        when(cartItemOptionRepository.findAllByCartItemId(10L)).thenReturn(List.of());

        CartResponseDto response = cartService.addItemToCart(1L, request, false);

        assertThat(response).isNotNull();
        assertThat(existingItem.getQuantity()).isEqualTo(3);
        assertThat(existingItem.getSubtotal()).isEqualByComparingTo("36.00");
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    @DisplayName("addItemToCart - throws ResourceConflictException when adding item from different restaurant")
    void addItemToCart_differentRestaurant_throwsConflict() {
        AddToCartRequestDto request = new AddToCartRequestDto(20L, 200L, 1, null);

        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(20L);
        otherRestaurant.setStatus(RestaurantStatus.OPEN);

        Cart existingCart = new Cart();
        existingCart.setId(1L);
        existingCart.setUser(activeUser);
        existingCart.setRestaurant(restaurant); // restaurant ID is 10L
        existingCart.setStatus(CartStatus.ACTIVE);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(otherRestaurant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(existingCart));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request, false))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Cart contains items from another restaurant");
    }

    @Test
    @DisplayName("addItemToCart - replaces cart when clearExisting is true for different restaurant")
    void addItemToCart_differentRestaurant_clearExistingTrue_replacesCart() {
        AddToCartRequestDto request = new AddToCartRequestDto(20L, 200L, 1, null);

        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(20L);
        otherRestaurant.setName("Other Restaurant");
        otherRestaurant.setStatus(RestaurantStatus.OPEN);

        Dish otherDish = new Dish();
        otherDish.setId(200L);
        otherDish.setName("Burger");
        otherDish.setPrice(new BigDecimal("10.00"));
        otherDish.setStatus(DishStatus.AVAILABLE);
        otherDish.setRestaurant(otherRestaurant);

        Cart existingCart = new Cart();
        existingCart.setId(1L);
        existingCart.setUser(activeUser);
        existingCart.setRestaurant(restaurant);
        existingCart.setStatus(CartStatus.ACTIVE);

        CartItem oldItem = new CartItem();
        oldItem.setId(99L);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(otherRestaurant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(existingCart));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of(oldItem), List.of());
        when(cartRepository.save(any(Cart.class))).thenReturn(existingCart);
        when(dishRepository.findByIdAndDeletedAtIsNull(200L)).thenReturn(Optional.of(otherDish));
        when(dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(200L)).thenReturn(List.of());

        CartResponseDto response = cartService.addItemToCart(1L, request, true);

        assertThat(response).isNotNull();
        assertThat(existingCart.getRestaurant()).isEqualTo(otherRestaurant);
        verify(cartItemOptionRepository).deleteAllByCartItemIdIn(List.of(99L));
        verify(cartItemRepository).deleteAllByCartId(1L);
    }

    @Test
    @DisplayName("addItemToCart - throws IllegalArgumentException when restaurant is closed")
    void addItemToCart_closedRestaurant_throwsException() {
        AddToCartRequestDto request = new AddToCartRequestDto(10L, 100L, 1, null);
        restaurant.setStatus(RestaurantStatus.CLOSED);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Restaurant is closed");
    }

    @Test
    @DisplayName("addItemToCart - throws IllegalArgumentException when dish is unavailable")
    void addItemToCart_unavailableDish_throwsException() {
        AddToCartRequestDto request = new AddToCartRequestDto(10L, 100L, 1, null);
        dish.setStatus(DishStatus.ON_HOLD);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(new Cart());
        when(dishRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(dish));

        assertThatThrownBy(() -> cartService.addItemToCart(1L, request, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently unavailable");
    }

    @Test
    @DisplayName("updateCartItemQuantity - successfully updates quantity and subtotal")
    void updateCartItemQuantity_success() {
        UpdateCartItemRequestDto request = new UpdateCartItemRequestDto(4);

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(activeUser);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setId(10L);
        item.setCart(cart);
        item.setDish(dish);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("12.00"));
        item.setSubtotal(new BigDecimal("24.00"));

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartItemOptionRepository.findAllByCartItemId(10L)).thenReturn(List.of());
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of(item));

        CartResponseDto response = cartService.updateCartItemQuantity(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getSubtotal()).isEqualByComparingTo("48.00");
        verify(cartItemRepository).save(item);
    }

    @Test
    @DisplayName("updateCartItemQuantity - removes item when quantity is 0")
    void updateCartItemQuantity_zeroQuantity_removesItem() {
        UpdateCartItemRequestDto request = new UpdateCartItemRequestDto(0);

        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(activeUser);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem item = new CartItem();
        item.setId(10L);
        item.setCart(cart);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of());

        CartResponseDto response = cartService.updateCartItemQuantity(1L, 10L, request);

        assertThat(response).isNotNull();
        verify(cartItemOptionRepository).deleteAllByCartItemId(10L);
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("removeCartItem - removes item and deletes cart if it was last item")
    void removeCartItem_lastItem_deletesCart() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setId(10L);
        item.setCart(cart);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of());

        CartResponseDto response = cartService.removeCartItem(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(response.totalItems()).isEqualTo(0);
        verify(cartRepository).delete(cart);
    }

    @Test
    @DisplayName("clearCart - deletes all items, options, and cart")
    void clearCart_success() {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUser(activeUser);

        CartItem item = new CartItem();
        item.setId(10L);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(activeUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(1L)).thenReturn(List.of(item));

        cartService.clearCart(1L);

        verify(cartItemOptionRepository).deleteAllByCartItemIdIn(List.of(10L));
        verify(cartItemRepository).deleteAllByCartId(1L);
        verify(cartRepository).delete(cart);
    }
}
