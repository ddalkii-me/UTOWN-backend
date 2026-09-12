package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.DeliveryAssignmentRepository;
import com.utown.utownbackend.repository.OrderRepository;
import com.utown.utownbackend.repository.RiderProfileRepository;
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceImplTest {

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RiderProfileRepository riderProfileRepository;

    @InjectMocks
    private DeliveryAssignmentServiceImpl deliveryAssignmentService;

    private User customer;
    private User riderUser;
    private RiderProfile riderProfile;
    private Restaurant restaurant;
    private Order order;
    private DeliveryAssignment deliveryAssignment;

    @BeforeEach
    void setUp() {
        customer = TestDataFactory.createUser(1L);
        customer.setRole(UserRole.CUSTOMER);

        riderUser = TestDataFactory.createUser(2L);
        riderUser.setRole(UserRole.RIDER);
        riderUser.setName("Speedy Rider");
        riderUser.setPhone("01099998888");

        riderProfile = TestDataFactory.createRiderProfile(
                10L, riderUser, TransportType.MOTORCYCLE, true, RiderStatus.ACTIVE
        );

        City city = TestDataFactory.createCity(1L, "Seoul");
        RestaurantType type = TestDataFactory.createRestaurantType(1L, "Pizza");
        User owner = TestDataFactory.createUser(3L);
        restaurant = TestDataFactory.createRestaurant(1L, "Pizza Palace", city, type, owner);

        order = TestDataFactory.createOrder(100L, customer, restaurant, null, OrderStatus.ACCEPTED);

        deliveryAssignment = TestDataFactory.createDeliveryAssignment(
                50L, order, riderProfile, DeliveryAssignmentStatus.ASSIGNED
        );
    }

    @Nested
    @DisplayName("Create Assignment Tests")
    class CreateAssignmentTests {

        @Test
        @DisplayName("createAssignment - should create assignment successfully")
        void createAssignment_shouldSucceed() {
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 10L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));
            when(deliveryAssignmentRepository.existsByOrderIdAndStatusIn(eq(100L), any())).thenReturn(false);
            when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(i -> {
                DeliveryAssignment da = i.getArgument(0);
                da.setId(50L);
                return da;
            });

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.createAssignment(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(50L);
            assertThat(response.orderId()).isEqualTo(100L);
            assertThat(response.orderNumber()).isEqualTo("ORD-100");
            assertThat(response.restaurantName()).isEqualTo("Pizza Palace");
            assertThat(response.riderId()).isEqualTo(10L);
            assertThat(response.riderName()).isEqualTo("Speedy Rider");
            assertThat(response.riderPhone()).isEqualTo("01099998888");
            assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.ASSIGNED);
            assertThat(response.assignedAt()).isNotNull();

            verify(deliveryAssignmentRepository).save(any(DeliveryAssignment.class));
        }

        @Test
        @DisplayName("createAssignment - should throw EntityNotFoundException when order does not exist")
        void createAssignment_orderNotFound_shouldThrow() {
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(999L, 10L);

            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Order not found");

            verify(deliveryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("createAssignment - should throw IllegalStateException when order status is not eligible")
        void createAssignment_orderNotEligible_shouldThrow() {
            order.setStatus(OrderStatus.PENDING);
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 10L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not ready for delivery");

            verify(deliveryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("createAssignment - should throw ResourceConflictException when active assignment exists")
        void createAssignment_activeAssignmentExists_shouldThrow() {
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 10L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(deliveryAssignmentRepository.existsByOrderIdAndStatusIn(eq(100L), any())).thenReturn(true);

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("already has an active delivery assignment");

            verify(deliveryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("createAssignment - should throw EntityNotFoundException when rider does not exist")
        void createAssignment_riderNotFound_shouldThrow() {
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 999L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(deliveryAssignmentRepository.existsByOrderIdAndStatusIn(eq(100L), any())).thenReturn(false);
            when(riderProfileRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Rider profile not found");

            verify(deliveryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("createAssignment - should throw IllegalStateException when rider is not active")
        void createAssignment_riderInactive_shouldThrow() {
            riderProfile.setStatus(RiderStatus.SUSPENDED);
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 10L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(deliveryAssignmentRepository.existsByOrderIdAndStatusIn(eq(100L), any())).thenReturn(false);
            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Rider is not active");

            verify(deliveryAssignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("createAssignment - should throw IllegalStateException when rider is not available")
        void createAssignment_riderUnavailable_shouldThrow() {
            riderProfile.setAvailability(false);
            DeliveryAssignmentRequestDto request = new DeliveryAssignmentRequestDto(100L, 10L);

            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(deliveryAssignmentRepository.existsByOrderIdAndStatusIn(eq(100L), any())).thenReturn(false);
            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            assertThatThrownBy(() -> deliveryAssignmentService.createAssignment(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not available");

            verify(deliveryAssignmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Lifecycle State Transitions")
    class StateTransitionTests {

        @Test
        @DisplayName("acceptAssignment - should transition ASSIGNED -> ACCEPTED")
        void acceptAssignment_shouldSucceed() {
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));
            when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.acceptAssignment(50L);

            assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.ACCEPTED);
            assertThat(response.acceptedAt()).isNotNull();
            assertThat(deliveryAssignment.getStatus()).isEqualTo(DeliveryAssignmentStatus.ACCEPTED);
        }

        @Test
        @DisplayName("acceptAssignment - should throw IllegalStateException if already ACCEPTED")
        void acceptAssignment_invalidState_shouldThrow() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));

            assertThatThrownBy(() -> deliveryAssignmentService.acceptAssignment(50L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot accept assignment");
        }

        @Test
        @DisplayName("pickupDelivery - should transition ACCEPTED -> PICKED_UP and update order")
        void pickupDelivery_shouldSucceed() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
            deliveryAssignment.setAcceptedAt(LocalDateTime.now().minusMinutes(5));

            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));
            when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.pickupDelivery(50L);

            assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.PICKED_UP);
            assertThat(response.pickedUpAt()).isNotNull();
            assertThat(order.getPickedUpAt()).isNotNull();
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("pickupDelivery - should throw IllegalStateException if still in ASSIGNED status")
        void pickupDelivery_invalidState_shouldThrow() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.ASSIGNED);
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));

            assertThatThrownBy(() -> deliveryAssignmentService.pickupDelivery(50L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot pickup delivery");
        }

        @Test
        @DisplayName("completeDelivery - should transition PICKED_UP -> DELIVERED and update order status")
        void completeDelivery_shouldSucceed() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.PICKED_UP);
            deliveryAssignment.setPickedUpAt(LocalDateTime.now().minusMinutes(10));

            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));
            when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.completeDelivery(50L);

            assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.DELIVERED);
            assertThat(response.deliveredAt()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(order.getDeliveredAt()).isNotNull();
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("completeDelivery - should throw IllegalStateException if status is not PICKED_UP")
        void completeDelivery_invalidState_shouldThrow() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));

            assertThatThrownBy(() -> deliveryAssignmentService.completeDelivery(50L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot complete delivery");
        }

        @Test
        @DisplayName("cancelAssignment - should transition ASSIGNED -> CANCELLED")
        void cancelAssignment_shouldSucceed() {
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));
            when(deliveryAssignmentRepository.save(any(DeliveryAssignment.class))).thenAnswer(i -> i.getArgument(0));

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.cancelAssignment(50L);

            assertThat(response.status()).isEqualTo(DeliveryAssignmentStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancelAssignment - should throw IllegalStateException if already DELIVERED")
        void cancelAssignment_alreadyDelivered_shouldThrow() {
            deliveryAssignment.setStatus(DeliveryAssignmentStatus.DELIVERED);
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));

            assertThatThrownBy(() -> deliveryAssignmentService.cancelAssignment(50L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot cancel delivery assignment");
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("getAssignmentById - should return DTO when found")
        void getAssignmentById_shouldReturnDto() {
            when(deliveryAssignmentRepository.findById(50L)).thenReturn(Optional.of(deliveryAssignment));

            DeliveryAssignmentResponseDto response = deliveryAssignmentService.getAssignmentById(50L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(50L);
            assertThat(response.orderId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("getAssignmentById - should throw EntityNotFoundException when not found")
        void getAssignmentById_notFound_shouldThrow() {
            when(deliveryAssignmentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryAssignmentService.getAssignmentById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Delivery assignment not found");
        }

        @Test
        @DisplayName("getAssignments - should return list")
        void getAssignments_shouldReturnList() {
            when(deliveryAssignmentRepository.findByRiderId(10L)).thenReturn(List.of(deliveryAssignment));

            List<DeliveryAssignmentResponseDto> results = deliveryAssignmentService.getAssignments(10L, null, null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).id()).isEqualTo(50L);
        }
    }
}
