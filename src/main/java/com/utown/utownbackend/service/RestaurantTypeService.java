package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;

import java.util.List;

public interface RestaurantTypeService {

    RestaurantTypeResponseDto createRestaurantType(RestaurantTypeRequestDto request);

    List<RestaurantTypeResponseDto> getAllRestaurantTypes();

    RestaurantTypeResponseDto getRestaurantTypeById(Long id);

    RestaurantTypeResponseDto updateRestaurantType(
            Long id,
            RestaurantTypeRequestDto request
    );

    void deleteRestaurantType(Long id);
}