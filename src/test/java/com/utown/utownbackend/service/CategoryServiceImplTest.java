package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.dto.CategoryResponseDto;
import com.utown.utownbackend.entity.Category;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.CategoryRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private DishRepository dishRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Restaurant restaurant;
    private Category category;
    private CategoryRequestDto requestDto;

    @BeforeEach
    void setUp() {
        restaurant = TestDataFactory.createRestaurant(1L, "Test Restaurant", null, null, null);
        category = TestDataFactory.createCategory(1L, "Burgers", restaurant);
        category.setDescription("Delicious burgers");
        category.setImageUrl("url");

        requestDto = new CategoryRequestDto(1L, "Burgers", "Delicious burgers", "url", 1);
    }

    // ── createCategory ───────────────────────────────────────────────

    @Test
    @DisplayName("createCategory - should return saved category")
    void createCategory_shouldReturnSavedCategory() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponseDto result = categoryService.createCategory(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Burgers");
        verify(restaurantRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory - should throw when restaurant not found")
    void createCategory_shouldThrowWhenRestaurantNotFound() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ── getAllCategories ──────────────────────────────────────────────

    @Test
    @DisplayName("getAllCategories - should return list of active categories")
    void getAllCategories_shouldReturnList() {
        when(categoryRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(category));

        List<CategoryResponseDto> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Burgers");
    }

    // ── getCategoryById ──────────────────────────────────────────────

    @Test
    @DisplayName("getCategoryById - should return category when found")
    void getCategoryById_shouldReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponseDto result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCategoryById - should throw when not found")
    void getCategoryById_shouldThrowWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateCategory ───────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory - should update and return category")
    void updateCategory_shouldUpdateAndReturnCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        CategoryResponseDto result = categoryService.updateCategory(1L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Burgers");
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("updateCategory - should throw when category not found")
    void updateCategory_shouldThrowWhenCategoryNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("updateCategory - should throw when restaurant not found")
    void updateCategory_shouldThrowWhenRestaurantNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(1L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ── deleteCategory ───────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory - should soft-delete category")
    void deleteCategory_shouldSetDeletedAt() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(dishRepository.existsByCategoryIdAndDeletedAtIsNull(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        assertThat(category.getDeletedAt()).isNotNull();
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("deleteCategory - should throw when active dishes exist")
    void deleteCategory_shouldThrowWhenDishesExist() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(dishRepository.existsByCategoryIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(ResourceConflictException.class);
        assertThat(category.getDeletedAt()).isNull();
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteCategory - should throw when category not found")
    void deleteCategory_shouldThrowWhenNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
