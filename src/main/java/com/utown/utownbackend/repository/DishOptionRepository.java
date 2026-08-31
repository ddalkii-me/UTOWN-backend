package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DishOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DishOptionRepository extends JpaRepository<DishOption, Long> {

    List<DishOption> findAllByDeletedAtIsNull();

    List<DishOption> findAllByOptionGroupIdAndDeletedAtIsNull(Long optionGroupId);

    Optional<DishOption> findByIdAndDeletedAtIsNull(Long id);
}
