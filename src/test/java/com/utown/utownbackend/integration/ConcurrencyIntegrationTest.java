package com.utown.utownbackend.integration;

import com.utown.utownbackend.entity.DeliveryAssignment;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import com.utown.utownbackend.entity.Order;
import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.entity.PaymentMethod;
import com.utown.utownbackend.entity.PaymentStatus;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.Address;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.repository.DeliveryAssignmentRepository;
import com.utown.utownbackend.repository.OrderRepository;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.AddressRepository;
import com.utown.utownbackend.entity.DeliveryArea;
import com.utown.utownbackend.repository.DeliveryAreaRepository;
import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.TransportType;
import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.repository.RiderProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.utown.utownbackend.service.SocketIONotificationService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class ConcurrencyIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantTypeRepository restaurantTypeRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private DeliveryAreaRepository deliveryAreaRepository;

    @Autowired
    private RiderProfileRepository riderProfileRepository;

    @MockitoBean
    private SocketIONotificationService socketIONotificationService;

    private User testUser;
    private Restaurant testRestaurant;
    private Address testAddress;
    private RiderProfile testRider;

    @BeforeEach
    public void setup() {
        User user = new User();
        user.setName("Test User");
        user.setPassword("password");
        user.setEmail("test_concurrency_" + java.util.UUID.randomUUID() + "@example.com");
        user.setRole(UserRole.CUSTOMER);
        user.setPhone("01012345678");
        user.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(user);

        City city = new City();
        city.setName("City-" + java.util.UUID.randomUUID().toString());
        city = cityRepository.save(city);

        RestaurantType type = new RestaurantType();
        type.setName("Concurrency Type " + System.currentTimeMillis());
        type = restaurantTypeRepository.save(type);

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Concurrency Restaurant " + System.currentTimeMillis());
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(testUser);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(BigDecimal.TEN);
        testRestaurant = restaurantRepository.save(restaurant);

        DeliveryArea area = new DeliveryArea();
        area.setName("Concurrency Area");
        area.setCity(city);
        area = deliveryAreaRepository.save(area);

        Address address = new Address();
        address.setUser(testUser);
        address.setAddressLine("123 Concurrency St");
        address.setRecipientName("Concurrency Recipient");
        address.setDeliveryArea(area);
        address.setCity(city);
        testAddress = addressRepository.save(address);

        User riderUser = new User();
        riderUser.setName("Test Rider");
        riderUser.setPassword("password");
        riderUser.setEmail("test_rider_" + java.util.UUID.randomUUID() + "@example.com");
        riderUser.setRole(UserRole.RIDER);
        riderUser.setPhone("01087654321");
        riderUser.setStatus(UserStatus.ACTIVE);
        riderUser = userRepository.save(riderUser);

        RiderProfile rider = new RiderProfile();
        rider.setUser(riderUser);
        rider.setTransportType(TransportType.BICYCLE);
        rider.setAvailability(true);
        rider.setStatus(RiderStatus.ACTIVE);
        testRider = riderProfileRepository.save(rider);
    }

    @Test
    @DisplayName("Concurrent update on Order should throw ObjectOptimisticLockingFailureException")
    public void testOrderOptimisticLocking() {
        Order order = new Order();
        order.setOrderNumber(java.util.UUID.randomUUID().toString());
        order.setCurrency("KRW");
        order.setUser(testUser);
        order.setRestaurant(testRestaurant);
        order.setAddress(testAddress);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(BigDecimal.TEN);
        order.setDeliveryFee(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.valueOf(11));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        Long orderId = order.getId();

        // Simulate two different transactions fetching the same order
        Order orderTx1 = orderRepository.findById(orderId).orElseThrow();
        Order orderTx2 = orderRepository.findById(orderId).orElseThrow();

        // Transaction 1 updates the order and saves it
        orderTx1.setStatus(OrderStatus.ACCEPTED);
        orderRepository.save(orderTx1);

        // Transaction 2 tries to update the same order and save it
        orderTx2.setStatus(OrderStatus.CANCELLED);
        
        assertThatThrownBy(() -> orderRepository.save(orderTx2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Concurrent update on DeliveryAssignment should throw ObjectOptimisticLockingFailureException")
    public void testDeliveryAssignmentOptimisticLocking() {
        Order order = new Order();
        order.setOrderNumber(java.util.UUID.randomUUID().toString());
        order.setCurrency("KRW");
        order.setUser(testUser);
        order.setRestaurant(testRestaurant);
        order.setAddress(testAddress);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(BigDecimal.TEN);
        order.setDeliveryFee(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.valueOf(11));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(order);
        assignment.setRider(testRider);
        // assignment.setDeliveryFee(BigDecimal.ONE);
        assignment.setStatus(com.utown.utownbackend.entity.DeliveryAssignmentStatus.ASSIGNED);
        assignment = deliveryAssignmentRepository.save(assignment);

        Long assignmentId = assignment.getId();

        DeliveryAssignment tx1 = deliveryAssignmentRepository.findById(assignmentId).orElseThrow();
        DeliveryAssignment tx2 = deliveryAssignmentRepository.findById(assignmentId).orElseThrow();

        tx1.setStatus(DeliveryAssignmentStatus.ACCEPTED);
        deliveryAssignmentRepository.save(tx1);

        tx2.setStatus(com.utown.utownbackend.entity.DeliveryAssignmentStatus.CANCELLED);

        assertThatThrownBy(() -> deliveryAssignmentRepository.save(tx2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Concurrent updates from multiple threads should only allow one to succeed")
    public void testConcurrentUpdatesFromMultipleThreads() throws Exception {
        Order order = new Order();
        order.setOrderNumber(java.util.UUID.randomUUID().toString());
        order.setCurrency("KRW");
        order.setUser(testUser);
        order.setRestaurant(testRestaurant);
        order.setAddress(testAddress);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(BigDecimal.TEN);
        order.setDeliveryFee(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.valueOf(11));
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order = orderRepository.save(order);

        final Long orderId = order.getId();

        int threadCount = 2;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger optimisticLockFailureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // We use a separate component or just transactions manually.
        // Actually, simulating it using manually created threads and direct repository calls.
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Each thread runs its own transaction (using a new transaction template or we just rely on save() creating one)
                    // The easiest way without TransactionTemplate is to just read and save, which acts across transactions
                    Order o = orderRepository.findById(orderId).orElseThrow();
                    o.setStatus(OrderStatus.ACCEPTED);
                    orderRepository.save(o);
                    successCount.incrementAndGet();
                } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                    optimisticLockFailureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(optimisticLockFailureCount.get()).isEqualTo(1);
    }
}
