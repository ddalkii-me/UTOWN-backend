package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DishOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DishOptionGroupRepository extends JpaRepository<DishOptionGroup, Long> {

    @Query("SELECT g FROM DishOptionGroup g WHERE g.deletedAt IS NULL AND g.dish.deletedAt IS NULL ORDER BY g.sortOrder ASC")
    List<DishOptionGroup> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    @Query("SELECT g FROM DishOptionGroup g WHERE g.dish.id = :dishId AND g.deletedAt IS NULL AND g.dish.deletedAt IS NULL ORDER BY g.sortOrder ASC")
    List<DishOptionGroup> findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(@Param("dishId") Long dishId);

    boolean existsByDishIdAndDeletedAtIsNull(Long dishId);

    Optional<DishOptionGroup> findByIdAndDeletedAtIsNull(Long id);
}
