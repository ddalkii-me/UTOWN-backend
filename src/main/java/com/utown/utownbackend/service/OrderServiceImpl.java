package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
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
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

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
        User user = userRepository.findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found with id: " + request.restaurantId()));

        Address address = addressRepository.findByIdAndDeletedAtIsNull(request.addressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found with id: " + request.addressId()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Address does not belong to user");
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

            if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
                throw new IllegalArgumentException("Dish '" + dish.getName() + "' does not belong to restaurant '" + restaurant.getName() + "'");
            }

            List<DishOption> options = new ArrayList<>();
            BigDecimal optionsUnitSum = BigDecimal.ZERO;

            if (itemDto.optionIds() != null && !itemDto.optionIds().isEmpty()) {
                for (Long optionId : itemDto.optionIds()) {
                    DishOption dishOption = dishOptionRepository.findByIdAndDeletedAtIsNull(optionId)
                            .orElseThrow(() -> new EntityNotFoundException("Dish option not found with id: " + optionId));

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

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal itemUnitTotal = unitPrice.add(optionsUnitSum);
            BigDecimal itemSubtotal = itemUnitTotal.multiply(BigDecimal.valueOf(itemDto.quantity()));

            orderSubtotal = orderSubtotal.add(itemSubtotal);
            preparedItems.add(new PreparedItem(dish, itemDto.quantity(), options, unitPrice, itemSubtotal));
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

        return toOrderResponseDto(savedOrder, itemResponseDtos);
    }

    @Override
    public List<OrderResponseDto> getAllOrders() {
        return getOrders(null, null, null);
    }

    @Override
    public List<OrderResponseDto> getOrders(Long restaurantId, Long userId, List<OrderStatus> statuses) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (restaurantId != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        return toOrderResponseDto(order, getOrderItemResponseDtos(order.getId()));
    }

    @Override
    @Transactional
    public OrderResponseDto acceptOrder(Long id, OrderAcceptRequestDto request) {
        Order order = orderRepository.findById(id)
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

        return toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));
    }

    @Override
    @Transactional
    public OrderResponseDto startPreparation(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Order cannot be moved to preparation from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.IN_PREPARATION);

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.IN_PREPARATION, actor, null);

        return toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));
    }

    @Override
    @Transactional
    public OrderResponseDto completeOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        if (order.getStatus() != OrderStatus.IN_PREPARATION) {
            throw new IllegalStateException("Order cannot be completed from status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setDeliveredAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        User actor = resolveActor(savedOrder);
        recordStatusHistory(savedOrder, OrderStatus.COMPLETED, actor, null);

        return toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));
    }

    @Override
    @Transactional
    public OrderResponseDto declineOrder(Long id, OrderDeclineRequestDto request) {
        Order order = orderRepository.findById(id)
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

        return toOrderResponseDto(savedOrder, getOrderItemResponseDtos(savedOrder.getId()));
    }

    private User resolveActor(Order order) {
        if (order.getRestaurant() != null && order.getRestaurant().getOwner() != null) {
            return order.getRestaurant().getOwner();
        }
        return order.getUser();
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
