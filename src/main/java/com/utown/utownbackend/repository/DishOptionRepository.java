package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DishOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DishOptionRepository extends JpaRepository<DishOption, Long> {

    @Query("SELECT o FROM DishOption o WHERE o.deletedAt IS NULL AND o.dishOptionGroup.deletedAt IS NULL AND o.dishOptionGroup.dish.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<DishOption> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    @Query("SELECT o FROM DishOption o WHERE o.dishOptionGroup.id = :optionGroupId AND o.deletedAt IS NULL AND o.dishOptionGroup.deletedAt IS NULL AND o.dishOptionGroup.dish.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<DishOption> findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(@Param("optionGroupId") Long optionGroupId);

    boolean existsByOptionGroupIdAndDeletedAtIsNull(Long optionGroupId);

    Optional<DishOption> findByIdAndDeletedAtIsNull(Long id);
}
