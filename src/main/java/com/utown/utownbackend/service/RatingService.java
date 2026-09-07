package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RatingRequestDto;
import com.utown.utownbackend.dto.RatingResponseDto;

import java.util.List;

public interface RatingService {

    RatingResponseDto createRating(
            Long userId,
            Long restaurantId,
            Long orderId,
            RatingRequestDto request
    );

    List<RatingResponseDto> getRatingsByRestaurant(
            Long restaurantId
    );

    RatingResponseDto getRatingById(
            Long id
    );

    void deleteRating(
            Long id
    );
}