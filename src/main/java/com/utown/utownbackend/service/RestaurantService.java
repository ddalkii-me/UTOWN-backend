package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.dto.WorkingHoursDto;
import com.utown.utownbackend.entity.RestaurantWorkingHours;

import java.time.DayOfWeek;
import java.util.List;

public interface RestaurantService {

    RestaurantResponseDto createRestaurant(RestaurantRequestDto request);

    List<RestaurantResponseDto> getAllRestaurants();

    RestaurantResponseDto getRestaurantById(Long id);

    RestaurantResponseDto updateRestaurant(Long id, RestaurantRequestDto request);

    void deleteRestaurant(Long id);

    void updateWorkingHourForDay(Long restaurantId, DayOfWeek dayOfWeek, WorkingHoursDto dto);

    List<WorkingHoursDto> getWorkingHours(Long restaurantId);
}