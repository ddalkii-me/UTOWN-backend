package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemOptionRepository cartItemOptionRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;
    private final DishOptionRepository dishOptionRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        log.info("Executing getCart with userId={}", userId);
        User user = findActiveUser(userId);

        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isEmpty()) {
            return CartResponseDto.empty(userId);
        }

        Cart cart = cartOpt.get();
        if (isCartExpired(cart)) {
            return CartResponseDto.empty(userId);
        }

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponseDto addItemToCart(Long userId, AddToCartRequestDto request, boolean clearExisting) {
        log.info("Executing addItemToCart with userId={}, clearExisting={}", userId, clearExisting);
        User user = findActiveUser(userId);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(request.restaurantId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found with id: " + request.restaurantId()));

        if (restaurant.getStatus() == RestaurantStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot add to cart: Restaurant is closed");
        }

        Cart cart = cartRepository.findByUserId(user.getId()).orElse(null);

        if (cart != null) {
            if (isCartExpired(cart)) {
                deleteCartContents(cart);
                cart.setRestaurant(restaurant);
                cart.setStatus(CartStatus.ACTIVE);
            } else if (!cart.getRestaurant().getId().equals(restaurant.getId())) {
                if (clearExisting) {
                    deleteCartContents(cart);
                    cart.setRestaurant(restaurant);
                } else {
                    throw new ResourceConflictException("Cart contains items from another restaurant. Please clear your cart first.");
                }
            }
            cart.setExpiresAt(LocalDateTime.now().plusDays(1));
            cart = cartRepository.save(cart);
        } else {
            cart = new Cart();
            cart.setUser(user);
            cart.setRestaurant(restaurant);
            cart.setStatus(CartStatus.ACTIVE);
            cart.setExpiresAt(LocalDateTime.now().plusDays(1));
            cart = cartRepository.save(cart);
        }

        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(request.dishId())
                .orElseThrow(() -> new EntityNotFoundException("Dish not found with id: " + request.dishId()));

        if (dish.getStatus() != DishStatus.AVAILABLE) {
            throw new IllegalArgumentException("Dish '" + dish.getName() + "' is currently unavailable");
        }

        if (!dish.getRestaurant().getId().equals(restaurant.getId())) {
            throw new IllegalArgumentException("Dish '" + dish.getName() + "' does not belong to restaurant '" + restaurant.getName() + "'");
        }

        List<DishOption> validatedOptions = validateAndFetchOptions(dish, request.optionIds());
        BigDecimal optionsUnitSum = validatedOptions.stream()
                .map(DishOption::getAdditionalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Check if identical item (same dish + same options) already exists in cart for quantity merging
        List<CartItem> existingItems = cartItemRepository.findAllByCartId(cart.getId());
        Set<Long> targetOptionIds = validatedOptions.stream().map(DishOption::getId).collect(Collectors.toSet());

        List<CartItem> matchingDishItems = existingItems.stream()
                .filter(item -> item.getDish().getId().equals(dish.getId()))
                .toList();

        CartItem existingMatch = null;
        List<CartItemOption> existingMatchOptions = Collections.emptyList();

        if (!matchingDishItems.isEmpty()) {
            List<Long> matchingItemIds = matchingDishItems.stream()
                    .map(BaseEntity::getId)
                    .toList();
            List<CartItemOption> optionsForMatchingItems = cartItemOptionRepository.findAllByCartItemIdIn(matchingItemIds);
            Map<Long, List<CartItemOption>> optionsByItemId = optionsForMatchingItems.stream()
                    .filter(opt -> opt.getCartItem() != null && opt.getCartItem().getId() != null)
                    .collect(Collectors.groupingBy(opt -> opt.getCartItem().getId()));

            for (CartItem item : matchingDishItems) {
                List<CartItemOption> itemOptions = optionsByItemId.getOrDefault(item.getId(), Collections.emptyList());
                Set<Long> currentOptionIds = itemOptions.stream()
                        .map(opt -> opt.getDishOption().getId())
                        .collect(Collectors.toSet());
                if (currentOptionIds.equals(targetOptionIds)) {
                    existingMatch = item;
                    existingMatchOptions = itemOptions;
                    break;
                }
            }
        }

        if (existingMatch != null) {
            int newQuantity = existingMatch.getQuantity() + request.quantity();
            existingMatch.setQuantity(newQuantity);
            BigDecimal storedOptionsUnitSum = existingMatchOptions.stream()
                    .map(CartItemOption::getOptionPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal itemUnitTotal = existingMatch.getUnitPrice().add(storedOptionsUnitSum);
            existingMatch.setSubtotal(itemUnitTotal.multiply(BigDecimal.valueOf(newQuantity)));
            cartItemRepository.save(existingMatch);
        } else {
            BigDecimal unitPrice = dish.getPrice();
            BigDecimal itemUnitTotal = unitPrice.add(optionsUnitSum);
            BigDecimal itemSubtotal = itemUnitTotal.multiply(BigDecimal.valueOf(request.quantity()));

            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setDish(dish);
            newItem.setQuantity(request.quantity());
            newItem.setUnitPrice(unitPrice);
            newItem.setSubtotal(itemSubtotal);
            CartItem savedItem = cartItemRepository.save(newItem);

            for (DishOption option : validatedOptions) {
                CartItemOption itemOption = new CartItemOption();
                itemOption.setCartItem(savedItem);
                itemOption.setDishOption(option);
                itemOption.setOptionName(option.getName());
                itemOption.setOptionPrice(option.getAdditionalPrice());
                cartItemOptionRepository.save(itemOption);
            }
        }

        cart.setExpiresAt(LocalDateTime.now().plusDays(1));
        cartRepository.save(cart);

        log.info("Item added to cart for user {}: dish {}", userId, dish.getId());
        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponseDto updateCartItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequestDto request) {
        log.info("Executing updateCartItemQuantity with userId={}, cartItemId={}", userId, cartItemId);
        findActiveUser(userId);
        Cart cart = findActiveCartOrThrow(userId);

        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with id: " + cartItemId));

        if (request.quantity() == null || request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        List<CartItemOption> itemOptions = cartItemOptionRepository.findAllByCartItemId(item.getId());
        BigDecimal optionsUnitSum = itemOptions.stream()
                .map(CartItemOption::getOptionPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemUnitTotal = item.getUnitPrice().add(optionsUnitSum);
        item.setQuantity(request.quantity());
        item.setSubtotal(itemUnitTotal.multiply(BigDecimal.valueOf(request.quantity())));
        cartItemRepository.save(item);

        cart.setExpiresAt(LocalDateTime.now().plusDays(1));
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponseDto removeCartItem(Long userId, Long cartItemId) {
        log.info("Executing removeCartItem with userId={}, cartItemId={}", userId, cartItemId);
        findActiveUser(userId);
        Cart cart = findActiveCartOrThrow(userId);

        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found with id: " + cartItemId));

        cartItemOptionRepository.deleteAllByCartItemId(item.getId());
        cartItemRepository.delete(item);

        List<CartItem> remaining = cartItemRepository.findAllByCartId(cart.getId());
        if (remaining.isEmpty()) {
            cartRepository.delete(cart);
            return CartResponseDto.empty(userId);
        }

        cart.setExpiresAt(LocalDateTime.now().plusDays(1));
        cartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Executing clearCart with userId={}", userId);
        User user = findActiveUser(userId);

        Optional<Cart> cartOpt = cartRepository.findByUserId(user.getId());
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            boolean expired = isCartExpired(cart);
            deleteCartContents(cart);
            cartRepository.delete(cart);
            if (expired) {
                log.info("Expired cart cleared for user {}", userId);
            } else {
                log.info("Cart cleared for user {}", userId);
            }
        }
    }

    // --- Private Helper Methods ---

    private User findActiveUser(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User is not active");
        }
        return user;
    }

    private Cart findActiveCartOrThrow(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found for user id: " + userId));

        if (isCartExpired(cart)) {
            deleteCartContents(cart);
            cartRepository.delete(cart);
            throw new EntityNotFoundException("Cart has expired for user id: " + userId);
        }

        return cart;
    }

    private boolean isCartExpired(Cart cart) {
        return cart.getStatus() == CartStatus.EXPIRED ||
                (cart.getExpiresAt() != null && cart.getExpiresAt().isBefore(LocalDateTime.now()));
    }

    private void deleteCartContents(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        if (!items.isEmpty()) {
            List<Long> itemIds = items.stream().map(BaseEntity::getId).toList();
            cartItemOptionRepository.deleteAllByCartItemIdIn(itemIds);
            cartItemRepository.deleteAllByCartId(cart.getId());
        }
    }

    private List<DishOption> validateAndFetchOptions(Dish dish, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            validateOptionGroups(dish, Collections.emptyList());
            return Collections.emptyList();
        }

        Set<Long> uniqueOptionIds = new HashSet<>(optionIds);
        if (uniqueOptionIds.size() != optionIds.size()) {
            throw new IllegalArgumentException("Duplicate options provided for dish '" + dish.getName() + "'");
        }

        List<DishOption> options = new ArrayList<>();
        for (Long optionId : optionIds) {
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
        }

        validateOptionGroups(dish, options);
        return options;
    }

    private void validateOptionGroups(Dish dish, List<DishOption> options) {
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
    }

    private CartResponseDto buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        List<Long> itemIds = items.stream().map(BaseEntity::getId).toList();

        Map<Long, List<CartItemOption>> optionsByItemId = itemIds.isEmpty()
                ? Collections.emptyMap()
                : cartItemOptionRepository.findAllByCartItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getCartItem().getId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        List<CartItemResponseDto> itemDtos = new ArrayList<>();
        for (CartItem item : items) {
            List<CartItemOption> options = optionsByItemId.getOrDefault(item.getId(), Collections.emptyList());
            List<CartItemOptionResponseDto> optionDtos = options.stream()
                    .map(opt -> new CartItemOptionResponseDto(
                            opt.getId(),
                            opt.getDishOption().getId(),
                            opt.getOptionName(),
                            opt.getOptionPrice()
                    ))
                    .toList();

            itemDtos.add(new CartItemResponseDto(
                    item.getId(),
                    item.getDish().getId(),
                    item.getDish().getName(),
                    item.getDish().getImageUrl(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal(),
                    optionDtos
            ));

            totalAmount = totalAmount.add(item.getSubtotal());
            totalItems += item.getQuantity();
        }

        return new CartResponseDto(
                cart.getId(),
                cart.getUser().getId(),
                cart.getRestaurant().getId(),
                cart.getRestaurant().getName(),
                cart.getStatus(),
                totalAmount,
                totalItems,
                itemDtos,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}
