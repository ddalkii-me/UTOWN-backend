package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantMenuResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.CategoryRepository;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import com.utown.utownbackend.repository.DishRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @Mock
    private DishOptionRepository dishOptionRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    private Restaurant restaurant;
    private Category cat1;
    private Category cat2;
    private Dish dish1;
    private Dish dish2;
    private Dish dish3;
    private DishOptionGroup group1;
    private DishOption opt1;
    private DishOption opt2;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setName("Seoul Kitchen");
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(new BigDecimal("15000"));

        cat1 = new Category();
        cat1.setId(10L);
        cat1.setName("Main Course");
        cat1.setRestaurant(restaurant);
        cat1.setPriority(1);

        cat2 = new Category();
        cat2.setId(20L);
        cat2.setName("Beverages");
        cat2.setRestaurant(restaurant);
        cat2.setPriority(2);

        dish1 = new Dish();
        dish1.setId(100L);
        dish1.setName("Bibimbap");
        dish1.setRestaurant(restaurant);
        dish1.setCategory(cat1);
        dish1.setPrice(new BigDecimal("10000"));
        dish1.setStatus(DishStatus.AVAILABLE);
        dish1.setSortOrder(1);

        dish2 = new Dish();
        dish2.setId(101L);
        dish2.setName("Bulgogi");
        dish2.setRestaurant(restaurant);
        dish2.setCategory(cat1);
        dish2.setPrice(new BigDecimal("15000"));
        dish2.setStatus(DishStatus.AVAILABLE);
        dish2.setSortOrder(2);

        dish3 = new Dish();
        dish3.setId(102L);
        dish3.setName("Green Tea");
        dish3.setRestaurant(restaurant);
        dish3.setCategory(cat2);
        dish3.setPrice(new BigDecimal("3000"));
        dish3.setStatus(DishStatus.AVAILABLE);
        dish3.setSortOrder(1);

        group1 = new DishOptionGroup();
        group1.setId(1000L);
        group1.setDish(dish1);
        group1.setName("Spiciness");
        group1.setRequired(true);
        group1.setMinSelections(1);
        group1.setMaxSelections(1);
        group1.setSortOrder(1);

        opt1 = new DishOption();
        opt1.setId(5001L);
        opt1.setOptionGroup(group1);
        opt1.setName("Mild");
        opt1.setAdditionalPrice(BigDecimal.ZERO);
        opt1.setStatus(DishOptionStatus.AVAILABLE);
        opt1.setSortOrder(1);

        opt2 = new DishOption();
        opt2.setId(5002L);
        opt2.setOptionGroup(group1);
        opt2.setName("Extra Hot");
        opt2.setAdditionalPrice(new BigDecimal("500"));
        opt2.setStatus(DishOptionStatus.AVAILABLE);
        opt2.setSortOrder(2);
    }

    @Test
    @DisplayName("getRestaurantMenu - successfully aggregates categories, dishes, groups, and options with batch queries")
    void getRestaurantMenu_success() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(1L)).thenReturn(List.of(cat1, cat2));
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L)).thenReturn(List.of(dish1, dish2, dish3));
        when(dishOptionGroupRepository.findAllByDishIdInAndDeletedAtIsNullOrderBySortOrderAsc(List.of(100L, 101L, 102L)))
                .thenReturn(List.of(group1));
        when(dishOptionRepository.findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(List.of(1000L)))
                .thenReturn(List.of(opt1, opt2));

        RestaurantMenuResponseDto result = menuService.getRestaurantMenu(1L);

        assertThat(result).isNotNull();
        assertThat(result.restaurantId()).isEqualTo(1L);
        assertThat(result.restaurantName()).isEqualTo("Seoul Kitchen");
        assertThat(result.restaurantStatus()).isEqualTo(RestaurantStatus.OPEN);
        assertThat(result.minimumOrderAmount()).isEqualByComparingTo("15000");

        assertThat(result.categories()).hasSize(2);

        // Cat 1
        var catDto1 = result.categories().get(0);
        assertThat(catDto1.id()).isEqualTo(10L);
        assertThat(catDto1.name()).isEqualTo("Main Course");
        assertThat(catDto1.dishes()).hasSize(2);

        // Dish 1 in Cat 1
        var dishDto1 = catDto1.dishes().get(0);
        assertThat(dishDto1.id()).isEqualTo(100L);
        assertThat(dishDto1.name()).isEqualTo("Bibimbap");
        assertThat(dishDto1.optionGroups()).hasSize(1);

        // Group 1 in Dish 1
        var groupDto1 = dishDto1.optionGroups().get(0);
        assertThat(groupDto1.id()).isEqualTo(1000L);
        assertThat(groupDto1.name()).isEqualTo("Spiciness");
        assertThat(groupDto1.required()).isTrue();
        assertThat(groupDto1.options()).hasSize(2);
        assertThat(groupDto1.options().get(0).name()).isEqualTo("Mild");
        assertThat(groupDto1.options().get(1).name()).isEqualTo("Extra Hot");

        // Dish 2 in Cat 1 has no option groups
        var dishDto2 = catDto1.dishes().get(1);
        assertThat(dishDto2.id()).isEqualTo(101L);
        assertThat(dishDto2.optionGroups()).isEmpty();

        // Cat 2
        var catDto2 = result.categories().get(1);
        assertThat(catDto2.id()).isEqualTo(20L);
        assertThat(catDto2.dishes()).hasSize(1);
        assertThat(catDto2.dishes().get(0).id()).isEqualTo(102L);
        assertThat(catDto2.dishes().get(0).optionGroups()).isEmpty();

        // Verify batch queries (no N+1)
        verify(restaurantRepository).findByIdAndDeletedAtIsNull(1L);
        verify(categoryRepository).findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(1L);
        verify(dishRepository).findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L);
        verify(dishOptionGroupRepository).findAllByDishIdInAndDeletedAtIsNullOrderBySortOrderAsc(anyList());
        verify(dishOptionRepository).findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(anyList());
    }

    @Test
    @DisplayName("getRestaurantMenu - empty menu returns restaurant info and empty categories list")
    void getRestaurantMenu_emptyMenu() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(1L)).thenReturn(Collections.emptyList());
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L)).thenReturn(Collections.emptyList());

        RestaurantMenuResponseDto result = menuService.getRestaurantMenu(1L);

        assertThat(result).isNotNull();
        assertThat(result.restaurantId()).isEqualTo(1L);
        assertThat(result.categories()).isEmpty();

        verify(dishOptionGroupRepository, never()).findAllByDishIdInAndDeletedAtIsNullOrderBySortOrderAsc(anyList());
        verify(dishOptionRepository, never()).findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(anyList());
    }

    @Test
    @DisplayName("getRestaurantMenu - dishes with no option groups skips option repository call")
    void getRestaurantMenu_dishesWithNoOptionGroups() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(1L)).thenReturn(List.of(cat1));
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L)).thenReturn(List.of(dish1));
        when(dishOptionGroupRepository.findAllByDishIdInAndDeletedAtIsNullOrderBySortOrderAsc(List.of(100L)))
                .thenReturn(Collections.emptyList());

        RestaurantMenuResponseDto result = menuService.getRestaurantMenu(1L);

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).dishes().get(0).optionGroups()).isEmpty();

        verify(dishOptionRepository, never()).findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(anyList());
    }

    @Test
    @DisplayName("getRestaurantMenu - throws EntityNotFoundException when restaurant does not exist or is soft-deleted")
    void getRestaurantMenu_restaurantNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuService.getRestaurantMenu(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Restaurant not found");

        verifyNoInteractions(categoryRepository, dishRepository, dishOptionGroupRepository, dishOptionRepository);
    }
}
