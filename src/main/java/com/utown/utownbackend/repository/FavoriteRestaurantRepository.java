package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.FavoriteRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRestaurantRepository
        extends JpaRepository<FavoriteRestaurant, Long> {

    List<FavoriteRestaurant> findAllByUserIdAndDeletedAtIsNull(
            Long userId
    );

    Optional<FavoriteRestaurant> findByIdAndDeletedAtIsNull(
            Long id
    );

    Optional<FavoriteRestaurant>
    findByUserIdAndRestaurantIdAndDeletedAtIsNull(
            Long userId,
            Long restaurantId
    );

    Optional<FavoriteRestaurant>
    findByUserIdAndRestaurantId(
            Long userId,
            Long restaurantId
    );
}