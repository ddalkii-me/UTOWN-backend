package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishRequestDto;
import com.utown.utownbackend.dto.DishResponseDto;
import com.utown.utownbackend.entity.Category;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.repository.CategoryRepository;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
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

import com.utown.utownbackend.util.TestDataFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishServiceImplTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @InjectMocks
    private DishServiceImpl dishService;

    private Restaurant restaurant;
    private Category category;
    private Dish dish;
    private DishRequestDto requestDto;

    @BeforeEach
    void setUp() {
        restaurant = TestDataFactory.createRestaurant(1L, "Test Restaurant", null, null, null);
        category = TestDataFactory.createCategory(1L, "Test Category", restaurant);
        dish = TestDataFactory.createDish(1L, "Pizza", restaurant, category);

        requestDto = new DishRequestDto(1L, 1L, "Pizza", BigDecimal.valueOf(10.0), "Tasty", "url", DishStatus.AVAILABLE, 1);
    }

    // ── createDish ───────────────────────────────────────────────────

    @Test
    @DisplayName("createDish - should return saved dish")
    void createDish_shouldReturnSavedDish() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(dishRepository.save(any(Dish.class))).thenReturn(dish);

        DishResponseDto result = dishService.createDish(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Pizza");
        verify(restaurantRepository).findByIdAndDeletedAtIsNull(1L);
        verify(categoryRepository).findById(1L);
        verify(dishRepository).save(any(Dish.class));
    }

    @Test
    @DisplayName("createDish - should throw when category belongs to different restaurant")
    void createDish_shouldThrowWhenCategoryHasDifferentRestaurant() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(2L);
        Category otherCategory = new Category();
        otherCategory.setId(1L);
        otherCategory.setRestaurant(otherRestaurant);

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() -> dishService.createDish(requestDto))
                .isInstanceOf(IllegalArgumentException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    @Test
    @DisplayName("createDish - should throw when restaurant not found")
    void createDish_shouldThrowWhenRestaurantNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.createDish(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    @Test
    @DisplayName("createDish - should throw when category not found")
    void createDish_shouldThrowWhenCategoryNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.createDish(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    // ── getDishes ────────────────────────────────────────────────────

    @Test
    @DisplayName("getDishes - default should return active dishes")
    void getDishes_shouldReturnList() {
        when(dishRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Pizza");
    }

    @Test
    @DisplayName("getDishes - deleted=true should return deleted dishes")
    void getDishes_withDeletedTrue_shouldReturnDeletedDishes() {
        when(dishRepository.findAllByDeletedAtIsNotNull()).thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(null, true);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByDeletedAtIsNotNull();
        verify(dishRepository, never()).findAllByDeletedAtIsNull();
    }

    @Test
    @DisplayName("getDishes - with status filter should return filtered dishes")
    void getDishes_withStatusFilter_shouldReturnFilteredDishes() {
        when(dishRepository.findAllByStatusAndDeletedAtIsNull(DishStatus.AVAILABLE)).thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(DishStatus.AVAILABLE, false);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByStatusAndDeletedAtIsNull(DishStatus.AVAILABLE);
        verify(dishRepository, never()).findAllByDeletedAtIsNull();
    }

    @Test
    @DisplayName("getDishes - with restaurantId filter should call restaurant repository query")
    void getDishes_withRestaurantId_shouldFilterByRestaurant() {
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L))
                .thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(1L, null, null, false);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(1L);
    }

    @Test
    @DisplayName("getDishes - with restaurantId and categoryId filter should call restaurant & category query")
    void getDishes_withRestaurantIdAndCategoryId_shouldFilterByBoth() {
        when(dishRepository.findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(1L, 2L))
                .thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(1L, 2L, null, false);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(1L, 2L);
    }

    @Test
    @DisplayName("getDishes - with restaurantId, categoryId, and status filter should call combined query")
    void getDishes_withRestaurantIdCategoryIdAndStatus_shouldFilterAll() {
        when(dishRepository.findAllByRestaurantIdAndCategoryIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(1L, 2L, DishStatus.AVAILABLE))
                .thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(1L, 2L, DishStatus.AVAILABLE, false);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByRestaurantIdAndCategoryIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(1L, 2L, DishStatus.AVAILABLE);
    }

    @Test
    @DisplayName("getDishes - with categoryId only should call category query")
    void getDishes_withCategoryIdOnly_shouldFilterByCategory() {
        when(dishRepository.findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(2L))
                .thenReturn(List.of(dish));

        List<DishResponseDto> result = dishService.getDishes(null, 2L, null, false);

        assertThat(result).hasSize(1);
        verify(dishRepository).findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(2L);
    }

    // ── getDishById ──────────────────────────────────────────────────

    @Test
    @DisplayName("getDishById - should return dish when found")
    void getDishById_shouldReturnDish() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));

        DishResponseDto result = dishService.getDishById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDishById - should throw when not found")
    void getDishById_shouldThrowWhenNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.getDishById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateDish ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateDish - should update and return dish")
    void updateDish_shouldUpdateAndReturnDish() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(dishRepository.save(any(Dish.class))).thenReturn(dish);

        DishResponseDto result = dishService.updateDish(1L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Pizza");
        verify(dishRepository).save(dish);
    }

    @Test
    @DisplayName("updateDish - should throw when dish not found")
    void updateDish_shouldThrowWhenDishNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.updateDish(99L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    @Test
    @DisplayName("updateDish - should throw when restaurant not found")
    void updateDish_restaurantNotFound_throwsNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.updateDish(1L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    @Test
    @DisplayName("updateDish - should throw when category not found")
    void updateDish_categoryNotFound_throwsNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.updateDish(1L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    @Test
    @DisplayName("updateDish - should throw when category belongs to different restaurant")
    void updateDish_categoryDifferentRestaurant_throwsIllegalArgument() {
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setId(2L);
        Category otherCategory = new Category();
        otherCategory.setId(1L);
        otherCategory.setRestaurant(otherRestaurant);

        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(otherCategory));

        assertThatThrownBy(() -> dishService.updateDish(1L, requestDto))
                .isInstanceOf(IllegalArgumentException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    // ── deleteDish ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteDish - should soft-delete dish")
    void deleteDish_shouldSetDeletedAt() {
        when(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));

        dishService.deleteDish(1L);

        assertThat(dish.getDeletedAt()).isNotNull();
        verify(dishRepository).save(dish);
    }

    @Test
    @DisplayName("deleteDish - should throw when active option groups exist")
    void deleteDish_shouldThrowWhenOptionGroupsExist() {
        when(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> dishService.deleteDish(1L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(dish.getDeletedAt()).isNull();
        verify(dishRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteDish - should throw when dish not found")
    void deleteDish_shouldThrowWhenNotFound() {
        when(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(99L)).thenReturn(false);
        when(dishRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.deleteDish(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }

    // ── restoreDish ──────────────────────────────────────────────────

    @Test
    @DisplayName("restoreDish - should clear deletedAt")
    void restoreDish_shouldClearDeletedAt() {
        when(dishRepository.findByIdAndDeletedAtIsNotNull(1L)).thenReturn(Optional.of(dish));

        dishService.restoreDish(1L);

        assertThat(dish.getDeletedAt()).isNull();
        verify(dishRepository).save(dish);
    }

    @Test
    @DisplayName("restoreDish - should throw when deleted dish not found")
    void restoreDish_shouldThrowWhenNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNotNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishService.restoreDish(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishRepository, never()).save(any(Dish.class));
    }
}
