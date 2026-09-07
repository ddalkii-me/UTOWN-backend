package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishOptionGroup;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import com.utown.utownbackend.repository.DishRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utown.utownbackend.util.TestDataFactory;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishOptionGroupServiceImplTest {

    @Mock
    private DishOptionGroupRepository dishOptionGroupRepository;

    @Mock
    private DishRepository dishRepository;

    @Mock
    private DishOptionRepository dishOptionRepository;

    @InjectMocks
    private DishOptionGroupServiceImpl dishOptionGroupService;

    private Dish dish;
    private DishOptionGroup dishOptionGroup;
    private DishOptionGroupRequestDto requestDto;

    @BeforeEach
    void setUp() {
        dish = TestDataFactory.createDish(1L, "Test Dish", null, null);
        dishOptionGroup = TestDataFactory.createDishOptionGroup(1L, "Size", dish);

        requestDto = new DishOptionGroupRequestDto(1L, "Size", true, 1, 1, 1);
    }

    // ── createDishOptionGroup ────────────────────────────────────────

    @Test
    @DisplayName("createDishOptionGroup - should return saved group")
    void createDishOptionGroup_shouldReturnSavedGroup() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(dishOptionGroupRepository.save(any(DishOptionGroup.class))).thenReturn(dishOptionGroup);

        DishOptionGroupResponseDto result = dishOptionGroupService.createDishOptionGroup(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Size");
        verify(dishRepository).findByIdAndDeletedAtIsNull(1L);
        verify(dishOptionGroupRepository).save(any(DishOptionGroup.class));
    }

    @Test
    @DisplayName("createDishOptionGroup - should throw when minSelections > maxSelections")
    void createDishOptionGroup_shouldThrowWhenMinGreaterThanMax() {
        DishOptionGroupRequestDto invalidRequest = new DishOptionGroupRequestDto(1L, "Size", true, 2, 1, 1);

        assertThatThrownBy(() -> dishOptionGroupService.createDishOptionGroup(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }

    @Test
    @DisplayName("createDishOptionGroup - should throw when dish not found")
    void createDishOptionGroup_shouldThrowWhenDishNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.createDishOptionGroup(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }

    // ── getAllDishOptionGroups ────────────────────────────────────────

    @Test
    @DisplayName("getAllDishOptionGroups - should return list")
    void getAllDishOptionGroups_shouldReturnList() {
        when(dishOptionGroupRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(dishOptionGroup));

        List<DishOptionGroupResponseDto> result = dishOptionGroupService.getAllDishOptionGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Size");
    }

    // ── getDishOptionGroupsByDishId ──────────────────────────────────

    @Test
    @DisplayName("getDishOptionGroupsByDishId - should return list for valid dish")
    void getDishOptionGroupsByDishId_shouldReturnList() {
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(1L)).thenReturn(List.of(dishOptionGroup));

        List<DishOptionGroupResponseDto> result = dishOptionGroupService.getDishOptionGroupsByDishId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Size");
    }

    @Test
    @DisplayName("getDishOptionGroupsByDishId - should throw when dish not found")
    void getDishOptionGroupsByDishId_shouldThrowWhenDishNotFound() {
        when(dishRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.getDishOptionGroupsByDishId(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionGroupRepository, never()).findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(anyLong());
    }

    // ── getDishOptionGroupById ───────────────────────────────────────

    @Test
    @DisplayName("getDishOptionGroupById - should return group when found")
    void getDishOptionGroupById_shouldReturnGroup() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dishOptionGroup));

        DishOptionGroupResponseDto result = dishOptionGroupService.getDishOptionGroupById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDishOptionGroupById - should throw when not found")
    void getDishOptionGroupById_shouldThrowWhenNotFound() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.getDishOptionGroupById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateDishOptionGroup ────────────────────────────────────────

    @Test
    @DisplayName("updateDishOptionGroup - should update and return group")
    void updateDishOptionGroup_shouldUpdateAndReturnGroup() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dishOptionGroup));
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dish));
        when(dishOptionGroupRepository.save(any(DishOptionGroup.class))).thenReturn(dishOptionGroup);

        DishOptionGroupResponseDto result = dishOptionGroupService.updateDishOptionGroup(1L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Size");
        verify(dishOptionGroupRepository).save(dishOptionGroup);
    }

    @Test
    @DisplayName("updateDishOptionGroup - should throw when minSelections > maxSelections")
    void updateDishOptionGroup_shouldThrowWhenMinGreaterThanMax() {
        DishOptionGroupRequestDto invalidRequest = new DishOptionGroupRequestDto(1L, "Size", true, 3, 1, 1);

        assertThatThrownBy(() -> dishOptionGroupService.updateDishOptionGroup(1L, invalidRequest))
                .isInstanceOf(IllegalArgumentException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }

    @Test
    @DisplayName("updateDishOptionGroup - should throw when group not found")
    void updateDishOptionGroup_shouldThrowWhenGroupNotFound() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.updateDishOptionGroup(99L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }

    @Test
    @DisplayName("updateDishOptionGroup - should throw when dish not found")
    void updateDishOptionGroup_shouldThrowWhenDishNotFound() {
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dishOptionGroup));
        when(dishRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.updateDishOptionGroup(1L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }

    // ── deleteDishOptionGroup ────────────────────────────────────────

    @Test
    @DisplayName("deleteDishOptionGroup - should soft-delete group")
    void deleteDishOptionGroup_shouldSetDeletedAt() {
        when(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(dishOptionGroup));

        dishOptionGroupService.deleteDishOptionGroup(1L);

        assertThat(dishOptionGroup.getDeletedAt()).isNotNull();
        verify(dishOptionGroupRepository).save(dishOptionGroup);
    }

    @Test
    @DisplayName("deleteDishOptionGroup - should throw when active options exist")
    void deleteDishOptionGroup_shouldThrowWhenOptionsExist() {
        when(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> dishOptionGroupService.deleteDishOptionGroup(1L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(dishOptionGroup.getDeletedAt()).isNull();
        verify(dishOptionGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteDishOptionGroup - should throw when group not found")
    void deleteDishOptionGroup_shouldThrowWhenGroupNotFound() {
        when(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(99L)).thenReturn(false);
        when(dishOptionGroupRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dishOptionGroupService.deleteDishOptionGroup(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(dishOptionGroupRepository, never()).save(any(DishOptionGroup.class));
    }
}
