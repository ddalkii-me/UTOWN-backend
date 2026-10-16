package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantWorkingHoursRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class RestaurantLogoUpdateService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantWorkingHoursRepository workingHoursRepository;

    public RestaurantLogoUpdateService(
            RestaurantRepository restaurantRepository,
            RestaurantWorkingHoursRepository workingHoursRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.workingHoursRepository = workingHoursRepository;
    }

    @Transactional
    public RestaurantResponseDto updateLogoUrl(Long id, String logoKey) {
        Restaurant restaurant = restaurantRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Restaurant not found")
                );

        restaurant.setLogoUrl(logoKey);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        boolean isOpenNow = savedRestaurant.getStatus() == RestaurantStatus.OPEN
                && RestaurantServiceImpl.isHoursOpenAt(workingHoursRepository.findByRestaurantId(id), ZonedDateTime.now());

        return new RestaurantResponseDto(
                savedRestaurant.getId(),
                savedRestaurant.getOwner().getId(),
                savedRestaurant.getType().getId(),
                savedRestaurant.getCity().getId(),
                savedRestaurant.getName(),
                savedRestaurant.getDescription(),
                savedRestaurant.getAddress(),
                savedRestaurant.getPhone(),
                savedRestaurant.getLogoUrl(),
                savedRestaurant.getLatitude(),
                savedRestaurant.getLongitude(),
                savedRestaurant.getMinimumOrderAmount(),
                savedRestaurant.getStatus(),
                isOpenNow,
                savedRestaurant.getAverageRating(),
                savedRestaurant.getDeliveryTimeMinutes()
        );
    }
}
