package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.specification.RestaurantSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public Page<RestaurantResponseDto> searchRestaurants(
            Long cityId, 
            String type, 
            BigDecimal minRating, 
            String keyword, 
            Pageable pageable
    ) {
        Specification<Restaurant> spec = RestaurantSpecification.getSearchSpecification(
                cityId, type, minRating, keyword
        );

        return restaurantRepository.findAll(spec, pageable)
                .map(this::toDto);
    }

    private RestaurantResponseDto toDto(Restaurant restaurant) {
        return new RestaurantResponseDto(
                restaurant.getId(),
                restaurant.getOwner().getId(),
                restaurant.getType().getId(),
                restaurant.getCity().getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getLogoUrl(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getMinimumOrderAmount(),
                restaurant.getStatus(),
                restaurant.getAverageRating(),
                restaurant.getDeliveryTimeMinutes()
        );
    }
}
