package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(Long restaurantId);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(Long restaurantId, Long categoryId);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(Long restaurantId, DishStatus status);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByRestaurantIdAndCategoryIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(Long restaurantId, Long categoryId, DishStatus status);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(Long categoryId);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    Optional<Dish> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByStatusAndDeletedAtIsNull(DishStatus status);

    @EntityGraph(attributePaths = {"restaurant", "category"})
    List<Dish> findAllByDeletedAtIsNotNull();

    @EntityGraph(attributePaths = {"restaurant", "category"})
    Optional<Dish> findByIdAndDeletedAtIsNotNull(Long id);

    boolean existsByIdAndRestaurantOwnerIdAndDeletedAtIsNull(
            Long dishId,
            Long ownerId
    );

    boolean existsByIdAndRestaurantOwnerIdAndDeletedAtIsNotNull(
            Long dishId,
            Long ownerId
    );
}
