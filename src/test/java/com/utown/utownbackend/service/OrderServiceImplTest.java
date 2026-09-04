package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.OrderItemRequestDto;
import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderItemOptionRepository orderItemOptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private DishOptionRepository dishOptionRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Restaurant restaurant;
    private Address address;
    private Dish dish1;
    private DishOptionGroup optionGroup;
    private DishOption option1;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Pizza Place");

        address = new Address();
        address.setId(100L);
        address.setUser(user);

        dish1 = new Dish();
        dish1.setId(20L);
        dish1.setName("Pepperoni Pizza");
        dish1.setPrice(BigDecimal.valueOf(10.00));
        dish1.setRestaurant(restaurant);

        optionGroup = new DishOptionGroup();
        optionGroup.setId(30L);
        optionGroup.setDish(dish1);

        option1 = new DishOption();
        option1.setId(40L);
        option1.setName("Extra Cheese");
        option1.setAdditionalPrice(BigDecimal.valueOf(2.50));
        option1.setOptionGroup(optionGroup);
    }

    @Test
    void createOrder_success() {
        OrderItemRequestDto itemRequest = new OrderItemRequestDto(20L, 2, List.of(40L));
        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                10L,
                100L,
                "Ring bell",
                PaymentMethod.CARD,
                List.of(itemRequest)
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(dishRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(dish1));
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(40L)).thenReturn(Optional.of(option1));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(500L);
            return o;
        });

        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> {
            OrderItem item = invocation.getArgument(0);
            item.setId(600L);
            return item;
        });

        when(orderItemOptionRepository.save(any(OrderItemOption.class))).thenAnswer(invocation -> {
            OrderItemOption opt = invocation.getArgument(0);
            opt.setId(700L);
            return opt;
        });

        OrderResponseDto response = orderService.createOrder(orderRequest);

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(10L, response.restaurantId());
        assertEquals(100L, response.addressId());
        assertTrue(response.orderNumber().startsWith("ORD-"));
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(PaymentMethod.CARD, response.paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals("Ring bell", response.deliveryNote());
        // Unit price = 10.00, option = 2.50 -> item unit = 12.50 * 2 = 25.00
        assertEquals(BigDecimal.valueOf(25.00), response.subTotal());
        assertEquals(BigDecimal.ZERO, response.deliveryFee());
        assertEquals(BigDecimal.valueOf(25.00), response.totalAmount());
        assertEquals("USD", response.currency());

        assertEquals(1, response.items().size());
        var itemDto = response.items().get(0);
        assertEquals(600L, itemDto.id());
        assertEquals(20L, itemDto.dishId());
        assertEquals("Pepperoni Pizza", itemDto.dishName());
        assertEquals(BigDecimal.valueOf(10.00), itemDto.unitPrice());
        assertEquals(2, itemDto.quantity());
        assertEquals(BigDecimal.valueOf(25.00), itemDto.subTotal());

        assertEquals(1, itemDto.options().size());
        var optDto = itemDto.options().get(0);
        assertEquals(700L, optDto.id());
        assertEquals("Extra Cheese", optDto.optionName());
        assertEquals(BigDecimal.valueOf(2.50), optDto.optionPrice());

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(orderItemOptionRepository).save(any(OrderItemOption.class));
    }

    @Test
    void createOrder_userNotFound_throwsEntityNotFound() {
        OrderRequestDto orderRequest = new OrderRequestDto(
                999L,
                10L,
                100L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of()))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_restaurantNotFound_throwsEntityNotFound() {
        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                999L,
                100L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of()))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_addressNotFound_throwsEntityNotFound() {
        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                10L,
                999L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of()))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_addressNotBelongingToUser_throwsIllegalArgument() {
        User otherUser = new User();
        otherUser.setId(2L);
        Address otherAddress = new Address();
        otherAddress.setId(100L);
        otherAddress.setUser(otherUser);

        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                10L,
                100L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of()))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(otherAddress));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_dishBelongsToDifferentRestaurant_throwsIllegalArgument() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(99L);
        otherRestaurant.setName("Sushi Bar");

        Dish otherDish = new Dish();
        otherDish.setId(20L);
        otherDish.setName("Salmon Roll");
        otherDish.setPrice(BigDecimal.valueOf(12.00));
        otherDish.setRestaurant(otherRestaurant);

        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                10L,
                100L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of()))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(dishRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(otherDish));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_optionBelongsToDifferentDish_throwsIllegalArgument() {
        Dish otherDish = new Dish();
        otherDish.setId(77L);

        DishOptionGroup otherGroup = new DishOptionGroup();
        otherGroup.setId(88L);
        otherGroup.setDish(otherDish);

        DishOption otherOption = new DishOption();
        otherOption.setId(40L);
        otherOption.setName("Wasabi");
        otherOption.setAdditionalPrice(BigDecimal.ONE);
        otherOption.setOptionGroup(otherGroup);

        OrderRequestDto orderRequest = new OrderRequestDto(
                1L,
                10L,
                100L,
                null,
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(20L, 1, List.of(40L)))
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(restaurant));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(dishRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(dish1));
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(40L)).thenReturn(Optional.of(otherOption));

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_success() {
        Order o = new Order();
        o.setId(500L);
        o.setUser(user);
        o.setRestaurant(restaurant);
        o.setAddress(address);
        o.setOrderNumber("ORD-123");
        o.setStatus(OrderStatus.PENDING);
        o.setPaymentMethod(PaymentMethod.CASH);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setSubtotal(BigDecimal.valueOf(10.00));
        o.setDeliveryFee(BigDecimal.ZERO);
        o.setTotalAmount(BigDecimal.valueOf(10.00));
        o.setCurrency("USD");

        OrderItem item = new OrderItem();
        item.setId(600L);
        item.setOrder(o);
        item.setDish(dish1);
        item.setDishName(dish1.getName());
        item.setUnitPrice(dish1.getPrice());
        item.setQuantity(1);
        item.setSubtotal(dish1.getPrice());

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderResponseDto response = orderService.getOrderById(500L);

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals("ORD-123", response.orderNumber());
        assertEquals(1, response.items().size());
        assertEquals(600L, response.items().get(0).id());
    }

    @Test
    void getOrderById_notFound_throwsEntityNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void getAllOrders_success() {
        Order o = new Order();
        o.setId(500L);
        o.setUser(user);
        o.setRestaurant(restaurant);
        o.setAddress(address);
        o.setOrderNumber("ORD-123");
        o.setStatus(OrderStatus.PENDING);
        o.setPaymentMethod(PaymentMethod.CASH);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setSubtotal(BigDecimal.valueOf(10.00));
        o.setDeliveryFee(BigDecimal.ZERO);
        o.setTotalAmount(BigDecimal.valueOf(10.00));
        o.setCurrency("USD");

        OrderItem item = new OrderItem();
        item.setId(600L);
        item.setOrder(o);
        item.setDish(dish1);
        item.setDishName(dish1.getName());
        item.setUnitPrice(dish1.getPrice());
        item.setQuantity(1);
        item.setSubtotal(dish1.getPrice());

        when(orderRepository.findAll()).thenReturn(List.of(o));
        when(orderItemRepository.findAllByOrderIdIn(List.of(500L))).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        List<OrderResponseDto> responses = orderService.getAllOrders();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(500L, responses.get(0).id());
        assertEquals(1, responses.get(0).items().size());
    }
}
