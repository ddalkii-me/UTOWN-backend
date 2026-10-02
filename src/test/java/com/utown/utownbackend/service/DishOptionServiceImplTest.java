package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utown.utownbackend.util.TestDataFactory;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishOptionServiceImplTest {

    @Mock
    private DishOptionRepository dishOptionRepository;

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @InjectMocks
    private DishOptionServiceImpl dishOptionService;

    @Mock
    private CacheManager cacheManager;

    private DishOptionGroup group;
    private DishOption option;
    private DishOptionRequestDto requestDto;

    @BeforeEach
    void setUp() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(1L);

        Dish dish = TestDataFactory.createDish(
                1L,
                "Test Dish",
                null,
                null
        );
        dish.setRestaurant(restaurant);

        group = TestDataFactory.createDishOptionGroup(
                1L,
                "Size",
                dish
        );

        option = TestDataFactory.createDishOption(
                1L,
                "Large",
                group
        );

        requestDto = new DishOptionRequestDto(
                "Large",
                1L,
                BigDecimal.valueOf(100),
                1,
                DishOptionStatus.AVAILABLE
        );
    }

    // ── createDishOption ─────────────────────────────────────────────

    @Test
    @DisplayName("createDishOption - should return saved option")
    void createDishOption_shouldReturnSavedOption() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(group));
        when(dishOptionRepository.save(any(DishOption.class))).thenReturn(option);

        DishOptionResponseDto result = dishOptionService.createDishOption(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Large");
        verify(dishOptionGroupRepository).findByIdAndDeletedAtIsNull(1L);
        verify(dishOptionRepository).save(any(DishOption.class));
    }

    @Test
    @DisplayName("createDishOption - should throw when group not found")
    void createDishOption_shouldThrowWhenGroupNotFound() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.createDishOption(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionRepository, never()).save(any(DishOption.class));
    }

    // ── getAllDishOptions ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllDishOptions - should return list")
    void getAllDishOptions_shouldReturnList() {
        when(dishOptionRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(option));

        List<DishOptionResponseDto> result = dishOptionService.getAllDishOptions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Large");
    }

    // ── getDishOptionsByGroupId ──────────────────────────────────────

    @Test
    @DisplayName("getDishOptionsByGroupId - should return list for valid group")
    void getDishOptionsByGroupId_shouldReturnList() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(group));
        when(dishOptionRepository.findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(1L)).thenReturn(List.of(option));

        List<DishOptionResponseDto> result = dishOptionService.getDishOptionsByGroupId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Large");
    }

    @Test
    @DisplayName("getDishOptionsByGroupId - should throw when group not found")
    void getDishOptionsByGroupId_shouldThrowWhenGroupNotFound() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.getDishOptionsByGroupId(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionRepository, never()).findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(anyLong());
    }

    // ── getDishOptionById ────────────────────────────────────────────

    @Test
    @DisplayName("getDishOptionById - should return option when found")
    void getDishOptionById_shouldReturnOption() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(option));

        DishOptionResponseDto result = dishOptionService.getDishOptionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDishOptionById - should throw when not found")
    void getDishOptionById_shouldThrowWhenNotFound() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.getDishOptionById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateDishOption ─────────────────────────────────────────────

    @Test
    @DisplayName("updateDishOption - should update and return option")
    void updateDishOption_shouldUpdateAndReturnOption() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(option));
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(group));
        when(dishOptionRepository.save(any(DishOption.class))).thenReturn(option);

        DishOptionResponseDto result = dishOptionService.updateDishOption(1L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Large");
        verify(dishOptionRepository).save(option);
    }

    @Test
    @DisplayName("updateDishOption - should throw when option not found")
    void updateDishOption_shouldThrowWhenOptionNotFound() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.updateDishOption(99L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionRepository, never()).save(any(DishOption.class));
    }

    @Test
    @DisplayName("updateDishOption - should throw when group not found")
    void updateDishOption_shouldThrowWhenGroupNotFound() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(option));
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.updateDishOption(1L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionRepository, never()).save(any(DishOption.class));
    }

    // ── deleteDishOption ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteDishOption - should soft-delete option")
    void deleteDishOption_shouldSetDeletedAt() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(option));

        dishOptionService.deleteDishOption(1L);

        assertThat(option.getDeletedAt()).isNotNull();
        verify(dishOptionRepository).save(option);
    }

    @Test
    @DisplayName("deleteDishOption - should throw when not found")
    void deleteDishOption_shouldThrowWhenNotFound() {
        when(dishOptionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionService.deleteDishOption(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionRepository, never()).save(any(DishOption.class));
    }
}
