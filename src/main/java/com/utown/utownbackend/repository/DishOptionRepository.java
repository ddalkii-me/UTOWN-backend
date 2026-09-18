package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DishOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DishOptionRepository extends JpaRepository<DishOption, Long> {

    @Query("SELECT o FROM DishOption o WHERE o.deletedAt IS NULL AND o.optionGroup.deletedAt IS NULL AND o.optionGroup.dish.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<DishOption> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    @Query("SELECT o FROM DishOption o WHERE o.optionGroup.id = :optionGroupId AND o.deletedAt IS NULL AND o.optionGroup.deletedAt IS NULL AND o.optionGroup.dish.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<DishOption> findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(@Param("optionGroupId") Long optionGroupId);

    @Query("SELECT o FROM DishOption o WHERE o.optionGroup.id IN :optionGroupIds AND o.deletedAt IS NULL AND o.optionGroup.deletedAt IS NULL AND o.optionGroup.dish.deletedAt IS NULL ORDER BY o.sortOrder ASC")
    List<DishOption> findAllByOptionGroupIdInAndDeletedAtIsNullOrderBySortOrderAsc(@Param("optionGroupIds") List<Long> optionGroupIds);

    boolean existsByOptionGroupIdAndDeletedAtIsNull(Long optionGroupId);

    Optional<DishOption> findByIdAndDeletedAtIsNull(Long id);
}
