package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.*;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartItemOptionRepository cartItemOptionRepository;

    private User customer;
    private Restaurant restaurant;
    private Dish dish;
    private DishOption dishOption;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail("cart_customer@test.com");
        customer.setPhone("01099998888");
        customer.setName("Cart Customer");
        customer.setPassword("secret");
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(UserStatus.ACTIVE);
        entityManager.persist(customer);

        User owner = new User();
        owner.setEmail("cart_owner@test.com");
        owner.setPhone("01077776666");
        owner.setName("Cart Owner");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Daejeon");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Korean");
        entityManager.persist(type);

        restaurant = new Restaurant();
        restaurant.setName("Kimchi House");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        Category category = new Category();
        category.setName("Main");
        category.setRestaurant(restaurant);
        category.setPriority(1);
        entityManager.persist(category);

        dish = new Dish();
        dish.setName("Kimchi Stew");
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setPrice(new BigDecimal("9000"));
        dish.setStatus(DishStatus.AVAILABLE);
        dish.setSortOrder(1);
        entityManager.persist(dish);

        DishOptionGroup optionGroup = new DishOptionGroup();
        dishOption = new DishOption();
        optionGroup.setDish(dish);
        optionGroup.setName("Spice Level");
        optionGroup.setRequired(true);
        optionGroup.setMaxSelections(1);
        optionGroup.setMinSelections(1);
        entityManager.persist(optionGroup);

        dishOption.setOptionGroup(optionGroup);
        dishOption.setName("Extra Spicy");
        dishOption.setAdditionalPrice(BigDecimal.ZERO);
        dishOption.setStatus(DishOptionStatus.AVAILABLE);
        dishOption.setSortOrder(1);
        entityManager.persist(dishOption);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByUserId - eagerly fetches restaurant and user to eliminate N+1")
    void findByUserId_eagerlyFetchesRestaurantAndUser() {
        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        entityManager.persist(cart);

        entityManager.flush();
        entityManager.clear();

        Optional<Cart> found = cartRepository.findByUserId(customer.getId());

        assertThat(found).isPresent();
        assertThat(Hibernate.isInitialized(found.get().getUser())).isTrue();
        assertThat(Hibernate.isInitialized(found.get().getRestaurant())).isTrue();
    }

    @Test
    @DisplayName("findAllByCartId - eagerly fetches dish to eliminate N+1")
    void findAllByCartId_eagerlyFetchesDish() {
        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        entityManager.persist(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setDish(dish);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("9000"));
        item.setSubtotal(new BigDecimal("18000"));
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());

        assertThat(items).hasSize(1);
        assertThat(Hibernate.isInitialized(items.get(0).getDish())).isTrue();
    }

    @Test
    @DisplayName("findAllByCartItemIdIn - eagerly fetches dishOption to eliminate N+1")
    void findAllByCartItemIdIn_eagerlyFetchesDishOption() {
        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusHours(2));
        entityManager.persist(cart);

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setDish(dish);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("9000"));
        item.setSubtotal(new BigDecimal("18000"));
        entityManager.persist(item);

        CartItemOption itemOption = new CartItemOption();
        itemOption.setCartItem(item);
        itemOption.setDishOption(dishOption);
        itemOption.setOptionName("Extra Spicy");
        itemOption.setOptionPrice(BigDecimal.ZERO);
        entityManager.persist(itemOption);

        entityManager.flush();
        entityManager.clear();

        List<CartItemOption> options = cartItemOptionRepository.findAllByCartItemIdIn(List.of(item.getId()));

        assertThat(options).hasSize(1);
        assertThat(Hibernate.isInitialized(options.get(0).getDishOption())).isTrue();
    }
}
