package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    boolean existsByCategoryIdAndDeletedAtIsNull(Long categoryId);

    List<Dish> findAllByDeletedAtIsNull();

    Optional<Dish> findByIdAndDeletedAtIsNull(Long id);

    List<Dish> findAllByStatusAndDeletedAtIsNull(DishStatus status);

    List<Dish> findAllByDeletedAtIsNotNull();

    Optional<Dish> findByIdAndDeletedAtIsNotNull(Long id);
}
