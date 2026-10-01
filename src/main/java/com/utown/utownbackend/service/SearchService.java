package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;

public interface SearchService {

    Page<RestaurantResponseDto> searchRestaurants(
            Long cityId,
            String type,
            BigDecimal minRating,
            String keyword,
            Pageable pageable
    );
}
