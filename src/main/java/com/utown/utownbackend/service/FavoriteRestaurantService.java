package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.FavoriteRestaurantResponseDto;

import java.util.List;

public interface FavoriteRestaurantService {

    FavoriteRestaurantResponseDto addFavorite(
            Long userId,
            Long restaurantId
    );

    List<FavoriteRestaurantResponseDto> getFavoritesByUser(
            Long userId
    );

    void removeFavorite(
            Long userId,
            Long restaurantId
    );
}