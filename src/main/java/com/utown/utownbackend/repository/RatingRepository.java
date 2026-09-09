package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findAllByRestaurantIdAndDeletedAtIsNull(
            Long restaurantId
    );

    List<Rating> findAllByUserIdAndDeletedAtIsNull(
            Long userId
    );

    Optional<Rating> findByUserIdAndOrderIdAndDeletedAtIsNull(
            Long userId,
            Long orderId
    );

    Optional<Rating> findByIdAndDeletedAtIsNull(
            Long id
    );
}