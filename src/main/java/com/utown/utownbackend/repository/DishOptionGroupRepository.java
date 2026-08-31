package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DishOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DishOptionGroupRepository extends JpaRepository<DishOptionGroup, Long> {

    List<DishOptionGroup> findAllByDeletedAtIsNull();

    List<DishOptionGroup> findAllByDishIdAndDeletedAtIsNull(Long dishId);

    Optional<DishOptionGroup> findByIdAndDeletedAtIsNull(Long id);
}
