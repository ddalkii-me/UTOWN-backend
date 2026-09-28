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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private User customer;
    private Restaurant restaurant;
    private Address address;
    private Dish dish;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail("order_cust@test.com");
        customer.setPhone("01011112222");
        customer.setName("Order Customer");
        customer.setPassword("secret");
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(UserStatus.ACTIVE);
        entityManager.persist(customer);

        User owner = new User();
        owner.setEmail("order_owner@test.com");
        owner.setPhone("01033334444");
        owner.setName("Order Owner");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Gwangju");
        entityManager.persist(city);

        DeliveryArea deliveryArea = new DeliveryArea();
        deliveryArea.setName("Buk-gu");
        deliveryArea.setCity(city);
        entityManager.persist(deliveryArea);

        address = new Address();
        address.setUser(customer);
        address.setCity(city);
        address.setDeliveryArea(deliveryArea);
        address.setLabel("Home");
        address.setRecipientName("Customer");
        address.setPhone("010-1111-2222");
        address.setAddressLine("123 Street");
        address.setPostalCode("12345");
        address.setLatitude(new BigDecimal("35.1595"));
        address.setLongitude(new BigDecimal("126.8526"));
        entityManager.persist(address);

        RestaurantType type = new RestaurantType();
        type.setName("Korean");
        entityManager.persist(type);

        restaurant = new Restaurant();
        restaurant.setName("Bibimbap House");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        Category category = new Category();
        category.setName("Bowls");
        category.setRestaurant(restaurant);
        category.setPriority(1);
        entityManager.persist(category);

        dish = new Dish();
        dish.setName("Beef Bibimbap");
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setPrice(new BigDecimal("10000"));
        dish.setStatus(DishStatus.AVAILABLE);
        dish.setSortOrder(1);
        entityManager.persist(dish);

        entityManager.flush();
    }

    private Order createOrder(String orderNumber) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(customer);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("10000"));
        order.setDeliveryFee(new BigDecimal("2000"));
        order.setTotalAmount(new BigDecimal("12000"));
        order.setCurrency("KRW");
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setPaymentStatus(PaymentStatus.PENDING);
        return entityManager.persist(order);
    }

    @Test
    @DisplayName("findById - eagerly fetches user, restaurant, and address to eliminate N+1")
    void findById_eagerlyFetchesAssociations() {
        Order order = createOrder("ORD-001");
        entityManager.flush();
        entityManager.clear();

        Optional<Order> found = orderRepository.findById(order.getId());

        assertThat(found).isPresent();
        assertThat(Hibernate.isInitialized(found.get().getUser())).isTrue();
        assertThat(Hibernate.isInitialized(found.get().getRestaurant())).isTrue();
        assertThat(Hibernate.isInitialized(found.get().getAddress())).isTrue();
    }

    @Test
    @DisplayName("findAllByOrderId - eagerly fetches dish to eliminate N+1")
    void findAllByOrderId_eagerlyFetchesDish() {
        Order order = createOrder("ORD-002");

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setDish(dish);
        item.setDishName(dish.getName());
        item.setUnitPrice(dish.getPrice());
        item.setQuantity(1);
        item.setSubtotal(dish.getPrice());
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());

        assertThat(items).hasSize(1);
        assertThat(Hibernate.isInitialized(items.get(0).getDish())).isTrue();
    }

    @Test
    @DisplayName("findAllByOrderIdIn - eagerly fetches dish to eliminate N+1")
    void findAllByOrderIdIn_eagerlyFetchesDish() {
        Order order = createOrder("ORD-003");

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setDish(dish);
        item.setDishName(dish.getName());
        item.setUnitPrice(dish.getPrice());
        item.setQuantity(1);
        item.setSubtotal(dish.getPrice());
        entityManager.persist(item);

        entityManager.flush();
        entityManager.clear();

        List<OrderItem> items = orderItemRepository.findAllByOrderIdIn(List.of(order.getId()));

        assertThat(items).hasSize(1);
        assertThat(Hibernate.isInitialized(items.get(0).getDish())).isTrue();
    }
}
