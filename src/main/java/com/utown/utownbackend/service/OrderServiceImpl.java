package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.OrderItemOptionResponseDto;
import com.utown.utownbackend.dto.OrderItemResponseDto;
import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) {
            return List.of();
        }

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

    @Override
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        List<Long> itemIds = items.stream().map(BaseEntity::getId).toList();
        List<OrderItemOption> options = itemIds.isEmpty()
                ? List.of()
                : orderItemOptionRepository.findAllByOrderItemIdIn(itemIds);

        Map<Long, List<OrderItemOption>> optionsByItemId = options.stream()
                .collect(Collectors.groupingBy(opt -> opt.getOrderItem().getId()));

        List<OrderItemResponseDto> itemDtos = items.stream()
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

        return toOrderResponseDto(order, itemDtos);
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

