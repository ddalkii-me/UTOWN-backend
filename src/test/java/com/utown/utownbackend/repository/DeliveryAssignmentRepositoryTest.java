package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.*;
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
class DeliveryAssignmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    private User customer;
    private User riderUser;
    private RiderProfile riderProfile;
    private Restaurant restaurant;
    private Order order1;
    private Order order2;
    private DeliveryAssignment da1;
    private DeliveryAssignment da2;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setEmail("cust@test.com");
        customer.setPhone("01011110001");
        customer.setName("Customer");
        customer.setPassword("pwd123456");
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(UserStatus.ACTIVE);
        entityManager.persist(customer);

        riderUser = new User();
        riderUser.setEmail("rider@test.com");
        riderUser.setPhone("01011110002");
        riderUser.setName("Rider");
        riderUser.setPassword("pwd123456");
        riderUser.setRole(UserRole.RIDER);
        riderUser.setStatus(UserStatus.ACTIVE);
        entityManager.persist(riderUser);

        riderProfile = new RiderProfile();
        riderProfile.setUser(riderUser);
        riderProfile.setTransportType(TransportType.MOTORCYCLE);
        riderProfile.setAvailability(true);
        riderProfile.setStatus(RiderStatus.ACTIVE);
        entityManager.persist(riderProfile);

        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setPhone("01011110003");
        owner.setName("Owner");
        owner.setPassword("pwd123456");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Seoul");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Korean");
        type.setDescription("Korean food");
        entityManager.persist(type);

        restaurant = new Restaurant();
        restaurant.setName("Seoul Kitchen");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setPhone("02-123-4567");
        restaurant.setAddress("Gangnam-daero 123");
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        DeliveryArea area = new DeliveryArea();
        area.setName("Gangnam");
        area.setCity(city);
        entityManager.persist(area);

        Address address = new Address();
        address.setUser(customer);
        address.setCity(city);
        address.setDeliveryArea(area);
        address.setRecipientName("Customer");
        address.setPhone("01011110001");
        address.setAddressLine("Teheran-ro 123");
        entityManager.persist(address);

        order1 = createTestOrder(customer, restaurant, address, "ORD-1001", OrderStatus.ACCEPTED);
        entityManager.persist(order1);

        order2 = createTestOrder(customer, restaurant, address, "ORD-1002", OrderStatus.DELIVERED);
        entityManager.persist(order2);

        da1 = new DeliveryAssignment();
        da1.setOrder(order1);
        da1.setRider(riderProfile);
        da1.setStatus(DeliveryAssignmentStatus.ASSIGNED);
        da1.setAssignedAt(LocalDateTime.now());
        entityManager.persist(da1);

        da2 = new DeliveryAssignment();
        da2.setOrder(order2);
        da2.setRider(riderProfile);
        da2.setStatus(DeliveryAssignmentStatus.DELIVERED);
        da2.setAssignedAt(LocalDateTime.now().minusHours(2));
        da2.setAcceptedAt(LocalDateTime.now().minusHours(1).minusMinutes(50));
        da2.setPickedUpAt(LocalDateTime.now().minusHours(1).minusMinutes(30));
        da2.setDeliveredAt(LocalDateTime.now().minusHours(1));
        entityManager.persist(da2);

        entityManager.flush();
    }

    private Order createTestOrder(User user, Restaurant rest, Address address, String orderNumber, OrderStatus status) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setRestaurant(rest);
        order.setAddress(address);
        order.setStatus(status);
        order.setSubtotal(new BigDecimal("20000"));
        order.setDeliveryFee(new BigDecimal("3000"));
        order.setTotalAmount(new BigDecimal("23000"));
        order.setCurrency("KRW");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentStatus(PaymentStatus.PAID);
        return order;
    }

    @Test
    @DisplayName("findByRiderId - should return all assignments for a rider")
    void findByRiderId_shouldReturnAssignments() {
        List<DeliveryAssignment> results = deliveryAssignmentRepository.findByRiderId(riderProfile.getId());
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("findByOrderId - should return assignments for an order")
    void findByOrderId_shouldReturnAssignments() {
        List<DeliveryAssignment> results = deliveryAssignmentRepository.findByOrderId(order1.getId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(DeliveryAssignmentStatus.ASSIGNED);
    }

    @Test
    @DisplayName("findByStatus - should filter assignments by status")
    void findByStatus_shouldFilterCorrectly() {
        List<DeliveryAssignment> assigned = deliveryAssignmentRepository.findByStatus(DeliveryAssignmentStatus.ASSIGNED);
        assertThat(assigned).hasSize(1);
        assertThat(assigned.get(0).getId()).isEqualTo(da1.getId());

        List<DeliveryAssignment> delivered = deliveryAssignmentRepository.findByStatus(DeliveryAssignmentStatus.DELIVERED);
        assertThat(delivered).hasSize(1);
        assertThat(delivered.get(0).getId()).isEqualTo(da2.getId());
    }

    @Test
    @DisplayName("findByRiderIdAndStatus - should filter by rider and status")
    void findByRiderIdAndStatus_shouldFilterCorrectly() {
        List<DeliveryAssignment> results = deliveryAssignmentRepository.findByRiderIdAndStatus(
                riderProfile.getId(), DeliveryAssignmentStatus.ASSIGNED
        );
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(da1.getId());
    }

    @Test
    @DisplayName("existsByOrderIdAndStatusIn - should check active assignment existence")
    void existsByOrderIdAndStatusIn_shouldCheckActiveAssignments() {
        List<DeliveryAssignmentStatus> activeStatuses = List.of(
                DeliveryAssignmentStatus.ASSIGNED,
                DeliveryAssignmentStatus.ACCEPTED,
                DeliveryAssignmentStatus.PICKED_UP
        );

        boolean activeForOrder1 = deliveryAssignmentRepository.existsByOrderIdAndStatusIn(order1.getId(), activeStatuses);
        boolean activeForOrder2 = deliveryAssignmentRepository.existsByOrderIdAndStatusIn(order2.getId(), activeStatuses);

        assertThat(activeForOrder1).isTrue();
        assertThat(activeForOrder2).isFalse();
    }

    @Test
    @DisplayName("findFirstByOrderIdAndStatusIn - should find active assignment")
    void findFirstByOrderIdAndStatusIn_shouldFindActive() {
        List<DeliveryAssignmentStatus> activeStatuses = List.of(
                DeliveryAssignmentStatus.ASSIGNED,
                DeliveryAssignmentStatus.ACCEPTED,
                DeliveryAssignmentStatus.PICKED_UP
        );

        Optional<DeliveryAssignment> found = deliveryAssignmentRepository.findFirstByOrderIdAndStatusIn(order1.getId(), activeStatuses);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(da1.getId());
    }
}
