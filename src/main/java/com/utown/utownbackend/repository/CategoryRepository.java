package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = {"restaurant"})
    List<Category> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"restaurant"})
    List<Category> findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(Long restaurantId);

    @EntityGraph(attributePaths = {"restaurant"})
    Optional<Category> findByIdAndDeletedAtIsNull(Long id);
}