package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Rating;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    @EntityGraph(attributePaths = {"user", "restaurant", "order"})
    List<Rating> findAllByRestaurantIdAndDeletedAtIsNull(
            Long restaurantId
    );

    @EntityGraph(attributePaths = {"user", "restaurant", "order"})
    List<Rating> findAllByUserIdAndDeletedAtIsNull(
            Long userId
    );

    @EntityGraph(attributePaths = {"user", "restaurant", "order"})
    Optional<Rating> findByUserIdAndOrderIdAndDeletedAtIsNull(
            Long userId,
            Long orderId
    );

    @EntityGraph(attributePaths = {"user", "restaurant", "order"})
    Optional<Rating> findByIdAndDeletedAtIsNull(
            Long id
    );
}