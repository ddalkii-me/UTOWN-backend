package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantLogoUpdateService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantLogoUpdateService(
            RestaurantRepository restaurantRepository
    ) {
        this.restaurantRepository = restaurantRepository;
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
                savedRestaurant.getAverageRating(),
                savedRestaurant.getDeliveryTimeMinutes()
        );
    }
}
