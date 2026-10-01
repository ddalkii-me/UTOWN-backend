package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Restaurant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {

    @EntityGraph(attributePaths = {"owner", "type", "city"})
    List<Restaurant> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"owner", "type", "city"})
    Optional<Restaurant> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCityIdAndDeletedAtIsNull(Long cityId);
}