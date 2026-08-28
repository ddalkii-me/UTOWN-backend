package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findAllByDeletedAtIsNull();

    Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCityIdAndDeletedAtIsNull(Long cityId);
}