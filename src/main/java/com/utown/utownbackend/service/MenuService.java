package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantMenuResponseDto;

public interface MenuService {

    RestaurantMenuResponseDto getRestaurantMenu(Long restaurantId);
}
