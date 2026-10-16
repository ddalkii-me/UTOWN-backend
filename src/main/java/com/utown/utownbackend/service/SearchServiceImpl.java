package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.entity.BaseEntity;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.entity.RestaurantWorkingHours;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantWorkingHoursRepository;
import com.utown.utownbackend.repository.specification.RestaurantSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantWorkingHoursRepository workingHoursRepository;

    @Override
    public Page<RestaurantResponseDto> searchRestaurants(
            Long cityId, 
            String type, 
            BigDecimal minRating, 
            String keyword, 
            RestaurantStatus status,
            Pageable pageable
    ) {
        Specification<Restaurant> spec = RestaurantSpecification.getSearchSpecification(
                cityId, type, minRating, keyword, status
        );

        Page<Restaurant> restaurantPage = restaurantRepository.findAll(spec, pageable);
        List<Restaurant> content = restaurantPage.getContent();
        if (content.isEmpty()) {
            return restaurantPage.map(r -> toDto(r, false));
        }

        List<Long> ids = content.stream().map(BaseEntity::getId).toList();
        List<RestaurantWorkingHours> allHours = workingHoursRepository.findByRestaurantIdIn(ids);
        Map<Long, List<RestaurantWorkingHours>> hoursMap = allHours.stream()
                .collect(Collectors.groupingBy(h -> h.getRestaurant().getId()));

        ZonedDateTime now = ZonedDateTime.now();
        return restaurantPage.map(r -> {
            boolean isOpenNow = r.getStatus() == RestaurantStatus.OPEN
                    && RestaurantServiceImpl.isHoursOpenAt(hoursMap.get(r.getId()), now);
            return toDto(r, isOpenNow);
        });
    }

    private RestaurantResponseDto toDto(Restaurant restaurant, boolean isOpenNow) {
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
                isOpenNow,
                restaurant.getAverageRating(),
                restaurant.getDeliveryTimeMinutes()
        );
    }
}
