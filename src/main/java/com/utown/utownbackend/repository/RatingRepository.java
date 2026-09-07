package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findAllByRestaurantId(Long restaurantId);

    List<Rating> findAllByUserId(Long userId);

    Optional<Rating> findByUserIdAndOrderId(
            Long userId,
            Long orderId
    );
}