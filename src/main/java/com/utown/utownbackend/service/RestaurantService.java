package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import java.time.DayOfWeek;
import java.util.List;

public interface RestaurantService {

    RestaurantResponseDto createRestaurant(RestaurantRequestDto request);

    List<RestaurantResponseDto> getAllRestaurants();

    RestaurantResponseDto getRestaurantById(Long id);

    RestaurantResponseDto updateRestaurant(
            Long id,
            RestaurantOwnerUpdateRequestDto request
    );

    RestaurantResponseDto updateRestaurantAsAdmin(
            Long id,
            RestaurantAdminUpdateRequestDto request
    );

    void deleteRestaurant(Long id);

    void updateWorkingHourForDay(
            Long restaurantId,
            DayOfWeek dayOfWeek,
            WorkingHoursDto dto
    );

    List<WorkingHoursDto> getWorkingHours(Long restaurantId);
}