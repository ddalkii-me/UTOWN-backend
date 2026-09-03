package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantDeliveryAreaResponseDto;

import java.util.List;

public interface RestaurantDeliveryAreaService {

    RestaurantDeliveryAreaResponseDto addDeliveryArea(
            Long restaurantId,
            Long deliveryAreaId
    );

    List<RestaurantDeliveryAreaResponseDto> getDeliveryAreasByRestaurant(
            Long restaurantId
    );

    void removeDeliveryArea(
            Long restaurantId,
            Long deliveryAreaId
    );
}