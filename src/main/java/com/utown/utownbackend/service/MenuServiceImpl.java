package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final DishOptionRepository dishOptionRepository;

    @Override
    @Cacheable(cacheNames = "restaurantMenus", key = "#p0")
    public RestaurantMenuResponseDto getRestaurantMenu(Long restaurantId) {
        log.info("Executing getRestaurantMenu with restaurantId={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found with id: " + restaurantId));

        List<Category> categories = categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId);
        List<Dish> dishes = dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId);

        Set<Long> validCategoryIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<Dish> validDishes = dishes.stream()
                .filter(dish -> dish.getCategory() != null && validCategoryIds.contains(dish.getCategory().getId()))
                .toList();

        if (validDishes.isEmpty()) {
            List<MenuCategoryDto> emptyCategoryDtos = categories.stream()
                    .map(cat -> new MenuCategoryDto(
                            cat.getId(),
                            cat.getName(),
                            cat.getDescription(),
                            cat.getImageUrl(),
                            cat.getPriority(),
                            Collections.emptyList()
                    ))
                    .toList();

            return new RestaurantMenuResponseDto(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getStatus(),
                    restaurant.getMinimumOrderAmount(),
                    emptyCategoryDtos
            );
        }

        List<Long> dishIds = validDishes.stream().map(Dish::getId).toList();
        List<DishOptionGroup> optionGroups = dishOptionGroupRepository
                .findAllByDishIdInAndDeletedAtIsNullOrderBySortOrderAsc(dishIds);

        Map<Long, List<MenuOptionDto>> optionsByGroupId = new HashMap<>();
        if (!optionGroups.isEmpty()) {
            List<Long> groupIds = optionGroups.stream().map(DishOptionGroup::getId).toList();
            List<DishOption> options = dishOptionRepository
                    .findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(groupIds);

            for (DishOption option : options) {
                optionsByGroupId.computeIfAbsent(option.getOptionGroup().getId(), k -> new ArrayList<>())
                        .add(new MenuOptionDto(
                                option.getId(),
                                option.getName(),
                                option.getAdditionalPrice(),
                                option.getStatus(),
                                option.getSortOrder()
                        ));
            }
        }

        Map<Long, List<MenuOptionGroupDto>> groupsByDishId = new HashMap<>();
        for (DishOptionGroup group : optionGroups) {
            List<MenuOptionDto> groupOptions = optionsByGroupId.getOrDefault(group.getId(), Collections.emptyList());
            groupsByDishId.computeIfAbsent(group.getDish().getId(), k -> new ArrayList<>())
                    .add(new MenuOptionGroupDto(
                            group.getId(),
                            group.getName(),
                            group.getRequired(),
                            group.getMinSelections(),
                            group.getMaxSelections(),
                            group.getSortOrder(),
                            groupOptions
                    ));
        }

        Map<Long, List<MenuDishDto>> dishesByCategoryId = new HashMap<>();
        for (Dish dish : validDishes) {
            List<MenuOptionGroupDto> dishOptionGroups = groupsByDishId.getOrDefault(dish.getId(), Collections.emptyList());
            dishesByCategoryId.computeIfAbsent(dish.getCategory().getId(), k -> new ArrayList<>())
                    .add(new MenuDishDto(
                            dish.getId(),
                            dish.getName(),
                            dish.getDescription(),
                            dish.getPrice(),
                            dish.getImageUrl(),
                            dish.getStatus(),
                            dish.getSortOrder(),
                            dishOptionGroups
                    ));
        }

        List<MenuCategoryDto> categoryDtos = categories.stream()
                .map(cat -> new MenuCategoryDto(
                        cat.getId(),
                        cat.getName(),
                        cat.getDescription(),
                        cat.getImageUrl(),
                        cat.getPriority(),
                        dishesByCategoryId.getOrDefault(cat.getId(), Collections.emptyList())
                ))
                .toList();

        return new RestaurantMenuResponseDto(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getStatus(),
                restaurant.getMinimumOrderAmount(),
                categoryDtos
        );
    }
}
