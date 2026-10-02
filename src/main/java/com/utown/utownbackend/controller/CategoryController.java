package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.dto.CategoryResponseDto;
import com.utown.utownbackend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize(
            "hasRole('ADMIN') or @restaurantSecurity.isOwner(authentication, #request.restaurantId())"
    )
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto request) {
        log.debug("Entering createCategory method");

        CategoryResponseDto response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories(
            @RequestParam(required = false) Long restaurantId) {

        List<CategoryResponseDto> categories =
                categoryService.getAllCategories(restaurantId);

        return ResponseEntity.ok(categories);
    }
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<CategoryResponseDto> getCategoryById(
            @PathVariable Long id) {
        log.debug("Entering getCategoryById method with id={}", id);

        CategoryResponseDto response =
                categoryService.getCategoryById(id);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN') or @categorySecurity.isOwner(authentication, #id)"
    )
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto request) {
        log.debug("Entering updateCategory method with id={}", id);

        CategoryResponseDto response =
                categoryService.updateCategory(id, request);

        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize(
            "hasRole('ADMIN') or @categorySecurity.isOwner(authentication, #id)"
    )
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id) {
        log.debug("Entering deleteCategory method with id={}", id);

        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }
}