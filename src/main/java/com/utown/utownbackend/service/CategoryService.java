package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.dto.CategoryResponseDto;

import java.util.List;

public interface CategoryService {

    CategoryResponseDto createCategory(CategoryRequestDto request);

    List<CategoryResponseDto> getAllCategories();

    List<CategoryResponseDto> getAllCategories(Long restaurantId);

    CategoryResponseDto getCategoryById(Long id);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto request);

    void deleteCategory(Long id);
}