package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import com.utown.utownbackend.security.CustomUserDetails;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.utown.utownbackend.security.CustomUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemOptionRepository orderItemOptionRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final AddressRepository addressRepository;
    private final DishRepository dishRepository;
    private final DishOptionRepository dishOptionRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemOptionRepository cartItemOptionRepository;
    private final RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;
    private final SocketIONotificationService socketIONotificationService;

    private record PreparedItem(
            Dish dish,
            Integer quantity,
            List<DishOption> options,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto request) {
        log.info("Executing createOrder");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            if (principal.getRole() == UserRole.CUSTOMER && !principal.getId().equals(request.userId())) {
                throw new AccessDeniedException("Customers can only place orders for themselves");
            }
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found with id: " + request.restaurantId()));

        if (restaurant.getStatus() == RestaurantStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot place order: Restaurant is closed");
        }

        Address address = addressRepository.findByIdAndDeletedAtIsNull(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + request.addressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        if (address.getDeliveryArea() == null || !restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(restaurant.getId(), address.getDeliveryArea().getId())) {
            throw new IllegalArgumentException("Restaurant does not deliver to the selected address area");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        BigDecimal orderSubtotal = BigDecimal.ZERO;
        List<PreparedItem> preparedItems = new ArrayList<>();

        for (var itemDto : request.items()) {
            if (itemDto.quantity() == null || itemDto.quantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be greater than zero");
            }

            Dish dish = dishRepository.findByIdAndDeletedAtIsNull(itemDto.dishId())
                    .orElseThrow(() -> new EntityNotFoundException("Dish not found with id: " + itemDto.dishId()));

            if (dish.getStatus() != DishStatus.AVAILABLE) {
                throw new IllegalArgumentException("Dish '" + dish.getName() + "' is currently unavailable");
            }

            if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Dish '" + dish.getName() + "' does not belong to restaurant '" + restaurant.getName() + "'");
            }

            List<DishOption> options = new ArrayList<>();
            BigDecimal optionsUnitSum = BigDecimal.ZERO;

            if (itemDto.optionIds() != null && !itemDto.optionIds().isEmpty()) {
                Set<Long> uniqueOptionIds = new HashSet<>(itemDto.optionIds());
                if (uniqueOptionIds.size() != itemDto.optionIds().size()) {
                    throw new IllegalArgumentException("Duplicate options provided for dish '" + dish.getName() + "'");
                }

                for (Long optionId : itemDto.optionIds()) {
                    DishOption dishOption = dishOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Dish option not found with id: " + optionId));

                    if (dishOption.getStatus() != DishOptionStatus.AVAILABLE) {
                        throw new IllegalArgumentException("Dish option '" + dishOption.getName() + "' is currently unavailable");
                    }

                    if (dishOption.getOptionGroup().getDeletedAt() != null || dishOption.getOptionGroup().getDish().getDeletedAt() != null) {
                        throw new EntityNotFoundException("Dish option not found or inactive with id: " + optionId);
                    }

                    if (!dishOption.getOptionGroup().getDish().getId().equals(dish.getId())) {
                        throw new IllegalArgumentException("Dish option '" + dishOption.getName() + "' does not belong to dish '" + dish.getName() + "'");
                    }

                    options.add(dishOption);
                    optionsUnitSum = optionsUnitSum.add(dishOption.getAdditionalPrice());
                }
            }

            List<DishOptionGroup> activeGroups = dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(dish.getId());
            Map<Long, Long> groupSelectionCounts = options.stream()
                    .collect(Collectors.groupingBy(opt -> opt.getOptionGroup().getId(), Collectors.counting()));

            for (DishOptionGroup group : activeGroups) {
                long count = groupSelectionCounts.getOrDefault(group.getId(), 0L);

                if (Boolean.TRUE.equals(group.getRequired()) && count == 0) {
                    throw new IllegalArgumentException("Missing required option group '" + group.getName() + "' for dish '" + dish.getName() + "'");
                }
                if (group.getMinSelections() != null && count < group.getMinSelections()) {
                    throw new IllegalArgumentException("Minimum selections not met for group '" + group.getName() + "' in dish '" + dish.getName() + "'");
                }
                if (group.getMaxSelections() != null && count > group.getMaxSelections()) {
                    throw new IllegalArgumentException("Maximum selections exceeded for group '" + group.getName() + "' in dish '" + dish.getName() + "'");
                }
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal itemUnitTotal = unitPrice.add(optionsUnitSum);
            BigDecimal itemSubtotal = itemUnitTotal.multiply(BigDecimal.valueOf(itemDto.quantity()));

            orderSubtotal = orderSubtotal.add(itemSubtotal);
            preparedItems.add(new PreparedItem(dish, itemDto.quantity(), options, unitPrice, itemSubtotal));
        }

        if (restaurant.getMinimumOrderAmount() != null && orderSubtotal.compareTo(restaurant.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Order subtotal does not meet the restaurant's minimum order amount of " + restaurant.getMinimumOrderAmount());
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        BigDecimal totalAmount = orderSubtotal.add(deliveryFee);

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(orderSubtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setCurrency("USD");
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryNote(request.deliveryNote());

        Order savedOrder = orderRepository.save(order);
        recordStatusHistory(savedOrder, OrderStatus.PENDING, user, null);

        List<OrderItemResponseDto> itemResponseDtos = new ArrayList<>();

        for (PreparedItem prep : preparedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setDish(prep.dish());
            orderItem.setDishName(prep.dish().getName());
            orderItem.setUnitPrice(prep.unitPrice());
            orderItem.setQuantity(prep.quantity());
            orderItem.setSubtotal(prep.subtotal());

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);

            List<OrderItemOptionResponseDto> optionResponseDtos = new ArrayList<>();
            for (DishOption opt : prep.options()) {
                OrderItemOption orderItemOption = new OrderItemOption();
                orderItemOption.setOrderItem(savedOrderItem);
                orderItemOption.setDishOption(opt);
                orderItemOption.setOptionName(opt.getName());
                orderItemOption.setOptionPrice(opt.getAdditionalPrice());

                OrderItemOption savedOption = orderItemOptionRepository.save(orderItemOption);
                optionResponseDtos.add(new OrderItemOptionResponseDto(
                        savedOption.getId(),
                        savedOption.getOptionName(),
                        savedOption.getOptionPrice()
                ));
            }

            itemResponseDtos.add(new OrderItemResponseDto(
                    savedOrderItem.getId(),
                    prep.dish().getId(),
                    savedOrderItem.getDishName(),
                    savedOrderItem.getUnitPrice(),
                    savedOrderItem.getQuantity(),
                    savedOrderItem.getSubtotal(),
                    optionResponseDtos
            ));
        }

        OrderResponseDto response = toOrderResponseDto(savedOrder, itemResponseDtos);

        socketIONotificationService.sendNewOrder(
                restaurant.getOwner().getId(),
                response
        );

        return response;
    }


    @Override
    public List<OrderResponseDto> getOrders(
            Long restaurantId,
            Long userId,
            List<OrderStatus> statuses,
            Authentication authentication
    ) {
        log.info("Executing getOrders with restaurantId={}, userId={}, statuses={}", restaurantId, userId, statuses);
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (authentication == null
                    || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
                throw new AccessDeniedException("Authentication required");
            }

            if (principal.getRole() == UserRole.CUSTOMER) {
                // Customers can only see their own orders.
                if (userId != null && !userId.equals(principal.getId())) {
                    throw new AccessDeniedException("Customers can only view their own orders");
                }
                predicates.add(
                        cb.equal(root.get("user").get("id"), principal.getId())
                );

            } else if (principal.getRole() == UserRole.RESTAURANT_OWNER) {
                // Restaurant owners can only see orders from restaurants they own.
                predicates.add(
                        cb.equal(
                                root.get("restaurant").get("owner").get("id"),
                                principal.getId()
                        )
                );

                // If a restaurantId is provided, filter to that specific restaurant.
                if (restaurantId != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("restaurant").get("id"),
                                    restaurantId
                            )
                    );
                }
            } else if (principal.getRole() == UserRole.ADMIN) {
                // Admin can filter orders freely.
                if (restaurantId != null) {
                    predicates.add(
                            cb.equal(root.get("restaurant").get("id"), restaurantId)
                    );
                }

                if (userId != null) {
                    predicates.add(
                            cb.equal(root.get("user").get("id"), userId)
                    );
                }
            }

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Order> orders = orderRepository.findAll(spec);


        if (orders.isEmpty()) {
            return List.of();
        }

        return mapOrdersToDtos(orders);
    }


    @Override
    public OrderResponseDto getOrderById(Long id) {
        log.info("Executing getOrderById with id={}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        return toOrderResponseDto(order, getOrderItemResponseDtos(order.getId()));
    }

    @Override
    @Transactional
    public OrderResponseDto acceptOrder(Long id, OrderAcceptRequestDto request) {
        log.info("Executing acceptOrder with id={}", id);
        Order order = orderRepository.findWithUserAndRestaurantOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be accepted from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        order.setEstimatedCookingMinutes(request.estimatedCookingMinutes());

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.ACCEPTED, actor,
                "Estimated cooking time: " + request.estimatedCookingMinutes() + " mins");

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto startPreparation(Long id) {
        log.info("Executing startPreparation with id={}", id);
        Order order = orderRepository.findWithUserAndRestaurantOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Order cannot be moved to preparation from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.IN_PREPARATION);

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.IN_PREPARATION, actor, null);

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto completeOrder(Long id) {
        log.info("Executing completeOrder with id={}", id);
        Order order = orderRepository.findWithUserAndRestaurantOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order cannot be completed from status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.COMPLETED, actor, null);

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto declineOrder(Long id, OrderDeclineRequestDto request) {
        log.info("Executing declineOrder with id={}", id);
        Order order = orderRepository.findWithUserAndRestaurantOwnerById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be declined from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.DECLINED);
        order.setRejectedAt(LocalDateTime.now());
        order.setRejectionReason(request.reason());

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.DECLINED, actor, request.reason());

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto checkout(CheckoutRequestDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            if (principal.getRole() == UserRole.CUSTOMER && !principal.getId().equals(request.userId())) {
                throw new AccessDeniedException("Cannot checkout another user's cart");
            }
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));

        Cart cart = cartRepository.findByUserIdAndStatus(request.userId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("No active cart found for user with id: " + request.userId()));

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Address address = addressRepository.findByIdAndDeletedAtIsNull(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + request.addressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Address does not belong to user");
        }

        Restaurant restaurant = cart.getRestaurant();
        if (restaurant.getStatus() == RestaurantStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot place order: Restaurant is closed");
        }

        if (address.getDeliveryArea() == null || !restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(restaurant.getId(), address.getDeliveryArea().getId())) {
            throw new IllegalArgumentException("Restaurant does not deliver to the selected address area");
        }

        List<Long> cartItemIds = cartItems.stream().map(BaseEntity::getId).toList();
        List<CartItemOption> cartItemOptions = cartItemIds.isEmpty()
                ? List.of()
                : cartItemOptionRepository.findAllByCartItemIdIn(cartItemIds);
        Map<Long, List<CartItemOption>> optionsByCartItemId = cartItemOptions.stream()
                .collect(Collectors.groupingBy(opt -> opt.getCartItem().getId()));

        BigDecimal orderSubtotal = BigDecimal.ZERO;
        List<PreparedItem> preparedItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new IllegalArgumentException("Item quantity must be greater than zero");
            }

            Dish dish = dishRepository.findByIdAndDeletedAtIsNull(cartItem.getDish().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Dish not found with id: " + cartItem.getDish().getId()));

            if (dish.getStatus() != DishStatus.AVAILABLE) {
                throw new IllegalArgumentException("Dish '" + dish.getName() + "' is currently unavailable");
            }

            if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Dish '" + dish.getName() + "' does not belong to restaurant '" + restaurant.getName() + "'");
            }

            List<CartItemOption> itemOptions = optionsByCartItemId.getOrDefault(cartItem.getId(), List.of());
            List<DishOption> dishOptions = new ArrayList<>();
            BigDecimal optionsUnitSum = BigDecimal.ZERO;

            for (CartItemOption itemOpt : itemOptions) {
                DishOption dishOption = dishOptionRepository.findByIdAndDeletedAtIsNull(itemOpt.getDishOption().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Dish option not found with id: " + itemOpt.getDishOption().getId()));

                if (dishOption.getStatus() != DishOptionStatus.AVAILABLE) {
                    throw new IllegalArgumentException("Dish option '" + dishOption.getName() + "' is currently unavailable");
                }

                if (dishOption.getOptionGroup().getDeletedAt() != null || dishOption.getOptionGroup().getDish().getDeletedAt() != null) {
                    throw new EntityNotFoundException("Dish option not found or inactive with id: " + dishOption.getId());
                }

                if (!dishOption.getOptionGroup().getDish().getId().equals(dish.getId())) {
                    throw new IllegalArgumentException("Dish option '" + dishOption.getName() + "' does not belong to dish '" + dish.getName() + "'");
                }

                dishOptions.add(dishOption);
                optionsUnitSum = optionsUnitSum.add(dishOption.getAdditionalPrice());
            }

            List<DishOptionGroup> activeGroups = dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(dish.getId());
            Map<Long, Long> groupSelectionCounts = dishOptions.stream()
                    .collect(Collectors.groupingBy(opt -> opt.getOptionGroup().getId(), Collectors.counting()));

            for (DishOptionGroup group : activeGroups) {
                long count = groupSelectionCounts.getOrDefault(group.getId(), 0L);

                if (Boolean.TRUE.equals(group.getRequired()) && count == 0) {
                    throw new IllegalArgumentException("Missing required option group '" + group.getName() + "' for dish '" + dish.getName() + "'");
                }
                if (group.getMinSelections() != null && count < group.getMinSelections()) {
                    throw new IllegalArgumentException("Minimum selections not met for group '" + group.getName() + "' in dish '" + dish.getName() + "'");
                }
                if (group.getMaxSelections() != null && count > group.getMaxSelections()) {
                    throw new IllegalArgumentException("Maximum selections exceeded for group '" + group.getName() + "' in dish '" + dish.getName() + "'");
                }
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal itemUnitTotal = unitPrice.add(optionsUnitSum);
            BigDecimal itemSubtotal = itemUnitTotal.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            orderSubtotal = orderSubtotal.add(itemSubtotal);
            preparedItems.add(new PreparedItem(dish, cartItem.getQuantity(), dishOptions, unitPrice, itemSubtotal));
        }

        if (restaurant.getMinimumOrderAmount() != null && orderSubtotal.compareTo(restaurant.getMinimumOrderAmount()) < 0) {
            throw new IllegalArgumentException("Order subtotal does not meet the restaurant's minimum order amount of " + restaurant.getMinimumOrderAmount());
        }

        BigDecimal deliveryFee = BigDecimal.ZERO;
        BigDecimal totalAmount = orderSubtotal.add(deliveryFee);

        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(orderSubtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setCurrency("USD");
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setDeliveryNote(request.deliveryNote());

        Order savedOrder = orderRepository.save(order);

        List<OrderItemResponseDto> orderItemDtos = new ArrayList<>();
        for (PreparedItem prep : preparedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setDish(prep.dish());
            orderItem.setDishName(prep.dish().getName());
            orderItem.setUnitPrice(prep.unitPrice());
            orderItem.setQuantity(prep.quantity());
            orderItem.setSubtotal(prep.subtotal());

            OrderItem savedOrderItem = orderItemRepository.save(orderItem);

            List<OrderItemOptionResponseDto> optionDtos = new ArrayList<>();
            for (DishOption dishOption : prep.options()) {
                OrderItemOption orderItemOption = new OrderItemOption();
                orderItemOption.setOrderItem(savedOrderItem);
                orderItemOption.setDishOption(dishOption);
                orderItemOption.setOptionName(dishOption.getName());
                orderItemOption.setOptionPrice(dishOption.getAdditionalPrice());

                OrderItemOption savedItemOption = orderItemOptionRepository.save(orderItemOption);
                optionDtos.add(new OrderItemOptionResponseDto(savedItemOption.getId(), savedItemOption.getOptionName(), savedItemOption.getOptionPrice()));
            }

            orderItemDtos.add(new OrderItemResponseDto(
                    savedOrderItem.getId(),
                    savedOrderItem.getDish().getId(),
                    savedOrderItem.getDishName(),
                    savedOrderItem.getUnitPrice(),
                    savedOrderItem.getQuantity(),
                    savedOrderItem.getSubtotal(),
                    optionDtos
            ));
        }

        cart.setStatus(CartStatus.EXPIRED);
        cartRepository.save(cart);

        recordStatusHistory(savedOrder, OrderStatus.PENDING, user, "Order created via checkout");

        OrderResponseDto response = toOrderResponseDto(savedOrder, orderItemDtos);

        socketIONotificationService.sendNewOrder(
                restaurant.getOwner().getId(),
                response
        );

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto markReadyForPickup(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.IN_PREPARATION) {
            throw new IllegalStateException("Order cannot be marked ready for pickup from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setReadyAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.READY_FOR_PICKUP, actor, null);

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long id, OrderCancelRequestDto request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be cancelled from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setRejectionReason(request.reason());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        Order savedOrder = orderRepository.save(order);
        User actor = order.getUser();
        recordStatusHistory(savedOrder, OrderStatus.CANCELLED, actor, request.reason());

        OrderResponseDto response =
                toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));

        sendOrderStatusUpdate(savedOrder, response);

        return response;
    }

    private User resolveActor(Order order) {
        if (order.getRestaurant() != null && order.getRestaurant().getOwner() != null) {
            return order.getRestaurant().getOwner();
        }
        return order.getUser();
    }

    private void sendOrderStatusUpdate(
            Order order,
            OrderResponseDto response
    ) {
        socketIONotificationService.sendOrderStatusUpdated(
                order.getUser().getId(),
                response
        );
    }

    private void recordStatusHistory(Order order, OrderStatus status, User user, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setChangedByUser(user);
        history.setReason(reason);
        orderStatusHistoryRepository.save(history);
    }

    private List<OrderItemResponseDto> getOrderItemResponseDtos(Long orderId) {
        List<OrderItem> items = orderItemRepository.findAllByOrderId(orderId);
        List<Long> itemIds = items.stream().map(BaseEntity::getId).toList();
        List<OrderItemOption> options = itemIds.isEmpty()
                ? List.of()
                : orderItemOptionRepository.findAllByOrderItemIdIn(itemIds);

        Map<Long, List<OrderItemOption>> optionsByItemId = options.stream()
                .collect(Collectors.groupingBy(opt -> opt.getOrderItem().getId()));

        return items.stream()
                .map(item -> {
                    List<OrderItemOption> itemOpts = optionsByItemId.getOrDefault(item.getId(), List.of());
                    List<OrderItemOptionResponseDto> optDtos = itemOpts.stream()
                            .map(opt -> new OrderItemOptionResponseDto(opt.getId(), opt.getOptionName(), opt.getOptionPrice()))
                            .toList();
                    return new OrderItemResponseDto(
                            item.getId(),
                            item.getDish().getId(),
                            item.getDishName(),
                            item.getUnitPrice(),
                            item.getQuantity(),
                            item.getSubtotal(),
                            optDtos
                    );
                })
                .toList();
    }

    private List<OrderResponseDto> mapOrdersToDtos(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(BaseEntity::getId).toList();
        List<OrderItem> allItems = orderItemRepository.findAllByOrderIdIn(orderIds);

        List<Long> itemIds = allItems.stream().map(BaseEntity::getId).toList();
        List<OrderItemOption> allOptions = itemIds.isEmpty()
                ? List.of()
                : orderItemOptionRepository.findAllByOrderItemIdIn(itemIds);

        Map<Long, List<OrderItemOption>> optionsByItemId = allOptions.stream()
                .collect(Collectors.groupingBy(opt -> opt.getOrderItem().getId()));

        Map<Long, List<OrderItemResponseDto>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getOrder().getId(),
                        Collectors.mapping(item -> {
                            List<OrderItemOption> itemOpts = optionsByItemId.getOrDefault(item.getId(), List.of());
                            List<OrderItemOptionResponseDto> optDtos = itemOpts.stream()
                                    .map(opt -> new OrderItemOptionResponseDto(opt.getId(), opt.getOptionName(), opt.getOptionPrice()))
                                    .toList();
                            return new OrderItemResponseDto(
                                    item.getId(),
                                    item.getDish().getId(),
                                    item.getDishName(),
                                    item.getUnitPrice(),
                                    item.getQuantity(),
                                    item.getSubtotal(),
                                    optDtos
                            );
                        }, Collectors.toList())
                ));

        return orders.stream()
                .map(order -> toOrderResponseDto(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    private OrderResponseDto toOrderResponseDto(Order order, List<OrderItemResponseDto> items) {
        return new OrderResponseDto(
                order.getId(),
                order.getUser().getId(),
                order.getRestaurant().getId(),
                order.getAddress().getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getDeliveryNote(),
                order.getEstimatedCookingMinutes(),
                order.getRejectionReason(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getAcceptedAt(),
                order.getRejectedAt(),
                order.getReadyAt(),
                order.getPickedUpAt(),
                order.getDeliveredAt(),
                order.getCancelledAt(),
                items
        );
    }
}
