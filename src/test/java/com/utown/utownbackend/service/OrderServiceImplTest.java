package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

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

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartItemOptionRepository cartItemOptionRepository;

    @Mock
    private RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Restaurant restaurant;
    private Address address;
    private DeliveryArea deliveryArea;
    private Dish dish1;
    private DishOptionGroup optionGroup;
    private DishOption option1;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        User owner = new User();
        owner.setId(99L);

        restaurant = new Restaurant();
        restaurant.setId(10L);
        restaurant.setName("Pizza Place");
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(5.00));

        deliveryArea = new DeliveryArea();
        deliveryArea.setId(10L);
        deliveryArea.setName("Downtown");

        address = new Address();
        address.setId(100L);
        address.setUser(user);
        address.setDeliveryArea(deliveryArea);

        dish1 = new Dish();
        dish1.setId(20L);
        dish1.setName("Pepperoni Pizza");
        dish1.setPrice(BigDecimal.valueOf(10.00));
        dish1.setRestaurant(restaurant);
        dish1.setStatus(DishStatus.AVAILABLE);

        optionGroup = new DishOptionGroup();
        optionGroup.setId(30L);
        optionGroup.setDish(dish1);
        optionGroup.setRequired(false);

        option1 = new DishOption();
        option1.setId(40L);
        option1.setName("Extra Cheese");
        option1.setAdditionalPrice(BigDecimal.valueOf(2.50));
        option1.setOptionGroup(optionGroup);
        option1.setStatus(DishOptionStatus.AVAILABLE);

        lenient().when(restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(true);
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
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
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
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        OrderItem item = createTestOrderItem(600L, o);

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
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findAll(any(Specification.class))).thenReturn(List.of(o));
        when(orderItemRepository.findAllByOrderIdIn(List.of(500L))).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        List<OrderResponseDto> responses = orderService.getAllOrders();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(500L, responses.get(0).id());
        assertEquals(1, responses.get(0).items().size());
    }

    @Test
    void getOrders_withFilters_success() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findAll(any(Specification.class))).thenReturn(List.of(o));
        when(orderItemRepository.findAllByOrderIdIn(List.of(500L))).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        List<OrderResponseDto> responses = orderService.getOrders(10L, 1L, List.of(OrderStatus.PENDING));

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(500L, responses.get(0).id());
    }

    @Test
    void acceptOrder_success() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderAcceptRequestDto request = new OrderAcceptRequestDto(30);
        OrderResponseDto response = orderService.acceptOrder(500L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.ACCEPTED, response.status());
        assertEquals(30, response.estimatedCookingMinutes());
        assertNotNull(response.acceptedAt());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void acceptOrder_invalidStatus_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.ACCEPTED);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        OrderAcceptRequestDto request = new OrderAcceptRequestDto(30);
        assertThrows(IllegalStateException.class, () -> orderService.acceptOrder(500L, request));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void acceptOrder_notFound_throwsEntityNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        OrderAcceptRequestDto request = new OrderAcceptRequestDto(30);
        assertThrows(EntityNotFoundException.class, () -> orderService.acceptOrder(999L, request));
    }

    @Test
    void startPreparation_success() {
        Order o = createTestOrder(500L, OrderStatus.ACCEPTED);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderResponseDto response = orderService.startPreparation(500L);

        assertNotNull(response);
        assertEquals(OrderStatus.IN_PREPARATION, response.status());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void startPreparation_invalidStatus_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        assertThrows(IllegalStateException.class, () -> orderService.startPreparation(500L));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void completeOrder_success() {
        Order o = createTestOrder(500L, OrderStatus.IN_PREPARATION);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderResponseDto response = orderService.completeOrder(500L);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertNotNull(response.deliveredAt());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void completeOrder_invalidStatus_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        assertThrows(IllegalStateException.class, () -> orderService.completeOrder(500L));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void declineOrder_success() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderDeclineRequestDto request = new OrderDeclineRequestDto("Restaurant is closed");
        OrderResponseDto response = orderService.declineOrder(500L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.DECLINED, response.status());
        assertEquals("Restaurant is closed", response.rejectionReason());
        assertNotNull(response.rejectedAt());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void declineOrder_invalidStatus_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.IN_PREPARATION);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        OrderDeclineRequestDto request = new OrderDeclineRequestDto("Restaurant is closed");
        assertThrows(IllegalStateException.class, () -> orderService.declineOrder(500L, request));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void checkout_success() {
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, "Leave at door");

        Cart cart = new Cart();
        cart.setId(200L);
        cart.setUser(user);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem cartItem = new CartItem();
        cartItem.setId(300L);
        cartItem.setCart(cart);
        cartItem.setDish(dish1);
        cartItem.setQuantity(2);
        cartItem.setUnitPrice(BigDecimal.valueOf(10.00));
        cartItem.setSubtotal(BigDecimal.valueOf(25.00));

        CartItemOption cartOption = new CartItemOption();
        cartOption.setId(400L);
        cartOption.setCartItem(cartItem);
        cartOption.setDishOption(option1);
        cartOption.setOptionName("Extra Cheese");
        cartOption.setOptionPrice(BigDecimal.valueOf(2.50));

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of(cartItem));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(cartItemOptionRepository.findAllByCartItemIdIn(List.of(300L))).thenReturn(List.of(cartOption));
        when(dishRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(dish1));
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(40L)).thenReturn(Optional.of(option1));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(500L);
            return o;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> {
            OrderItem item = inv.getArgument(0);
            item.setId(600L);
            return item;
        });
        when(orderItemOptionRepository.save(any(OrderItemOption.class))).thenAnswer(inv -> {
            OrderItemOption opt = inv.getArgument(0);
            opt.setId(700L);
            return opt;
        });

        OrderResponseDto response = orderService.checkout(request);

        assertNotNull(response);
        assertEquals(500L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(10L, response.restaurantId());
        assertEquals(100L, response.addressId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(PaymentMethod.CARD, response.paymentMethod());
        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
        assertEquals("Leave at door", response.deliveryNote());
        assertEquals(BigDecimal.valueOf(25.00), response.subTotal());
        assertEquals(BigDecimal.valueOf(25.00), response.totalAmount());
        assertEquals(1, response.items().size());
        assertEquals("Pepperoni Pizza", response.items().get(0).dishName());
        assertEquals(1, response.items().get(0).options().size());
        assertEquals("Extra Cheese", response.items().get(0).options().get(0).optionName());

        assertEquals(CartStatus.EXPIRED, cart.getStatus());
        verify(cartRepository).save(cart);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void checkout_noActiveCart_throwsIllegalStateException() {
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, null);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> orderService.checkout(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_emptyCart_throwsIllegalStateException() {
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, null);
        Cart cart = new Cart();
        cart.setId(200L);
        cart.setUser(user);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> orderService.checkout(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_restaurantClosed_throwsIllegalArgumentException() {
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, null);
        Restaurant closedRestaurant = new Restaurant();
        closedRestaurant.setId(10L);
        closedRestaurant.setStatus(RestaurantStatus.CLOSED);

        Cart cart = new Cart();
        cart.setId(200L);
        cart.setUser(user);
        cart.setRestaurant(closedRestaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem cartItem = new CartItem();
        cartItem.setId(300L);
        cartItem.setCart(cart);
        cartItem.setDish(dish1);
        cartItem.setQuantity(1);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of(cartItem));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));

        assertThrows(IllegalArgumentException.class, () -> orderService.checkout(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_outsideDeliveryArea_throwsIllegalArgumentException() {
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, null);
        Cart cart = new Cart();
        cart.setId(200L);
        cart.setUser(user);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem cartItem = new CartItem();
        cartItem.setId(300L);
        cartItem.setCart(cart);
        cartItem.setDish(dish1);
        cartItem.setQuantity(1);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of(cartItem));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(10L, 10L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> orderService.checkout(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_belowMinimumOrderAmount_throwsIllegalArgumentException() {
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(50.00));
        CheckoutRequestDto request = new CheckoutRequestDto(1L, 100L, PaymentMethod.CARD, null);
        Cart cart = new Cart();
        cart.setId(200L);
        cart.setUser(user);
        cart.setRestaurant(restaurant);
        cart.setStatus(CartStatus.ACTIVE);

        CartItem cartItem = new CartItem();
        cartItem.setId(300L);
        cartItem.setCart(cart);
        cartItem.setDish(dish1);
        cartItem.setQuantity(1);

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartId(200L)).thenReturn(List.of(cartItem));
        when(addressRepository.findByIdAndDeletedAtIsNull(100L)).thenReturn(Optional.of(address));
        when(cartItemOptionRepository.findAllByCartItemIdIn(List.of(300L))).thenReturn(List.of());
        when(dishRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(dish1));

        assertThrows(IllegalArgumentException.class, () -> orderService.checkout(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrder_pending_success() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        o.setPaymentStatus(PaymentStatus.PAID);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderCancelRequestDto request = new OrderCancelRequestDto("Customer changed their mind");
        OrderResponseDto response = orderService.cancelOrder(500L, request);

        assertNotNull(response);
        assertEquals(OrderStatus.CANCELLED, response.status());
        assertEquals("Customer changed their mind", response.rejectionReason());
        assertNotNull(response.cancelledAt());
        assertEquals(PaymentStatus.REFUNDED, response.paymentStatus());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void cancelOrder_notPending_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.IN_PREPARATION);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        OrderCancelRequestDto request = new OrderCancelRequestDto("Changed mind");
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(500L, request));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void markReadyForPickup_inPreparation_success() {
        Order o = createTestOrder(500L, OrderStatus.IN_PREPARATION);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderResponseDto response = orderService.markReadyForPickup(500L);

        assertNotNull(response);
        assertEquals(OrderStatus.READY_FOR_PICKUP, response.status());
        assertNotNull(response.readyAt());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void markReadyForPickup_notInPreparation_throwsIllegalStateException() {
        Order o = createTestOrder(500L, OrderStatus.PENDING);
        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));

        assertThrows(IllegalStateException.class, () -> orderService.markReadyForPickup(500L));
        verify(orderRepository, never()).save(o);
    }

    @Test
    void completeOrder_readyForPickup_success() {
        Order o = createTestOrder(500L, OrderStatus.READY_FOR_PICKUP);
        OrderItem item = createTestOrderItem(600L, o);

        when(orderRepository.findById(500L)).thenReturn(Optional.of(o));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findAllByOrderId(500L)).thenReturn(List.of(item));
        when(orderItemOptionRepository.findAllByOrderItemIdIn(List.of(600L))).thenReturn(List.of());

        OrderResponseDto response = orderService.completeOrder(500L);

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertNotNull(response.deliveredAt());
        verify(orderRepository).save(o);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void createOrder_outsideDeliveryArea_throwsIllegalArgument() {
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
        when(restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(10L, 10L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(orderRequest));
        verify(orderRepository, never()).save(any());
    }

    private Order createTestOrder(Long id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUser(user);
        o.setRestaurant(restaurant);
        o.setAddress(address);
        o.setOrderNumber("ORD-123");
        o.setStatus(status);
        o.setPaymentMethod(PaymentMethod.CASH);
        o.setPaymentStatus(PaymentStatus.PENDING);
        o.setSubtotal(BigDecimal.valueOf(10.00));
        o.setDeliveryFee(BigDecimal.ZERO);
        o.setTotalAmount(BigDecimal.valueOf(10.00));
        o.setCurrency("USD");
        return o;
    }

    private OrderItem createTestOrderItem(Long id, Order order) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrder(order);
        item.setDish(dish1);
        item.setDishName(dish1.getName());
        item.setUnitPrice(dish1.getPrice());
        item.setQuantity(1);
        item.setSubtotal(dish1.getPrice());
        return item;
    }
}
