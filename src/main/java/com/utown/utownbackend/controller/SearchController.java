package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<Page<RestaurantResponseDto>> searchRestaurants(
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "averageRating", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RestaurantResponseDto> results = searchService.searchRestaurants(
                cityId, type, minRating, keyword, pageable
        );

        return ResponseEntity.ok(results);
    }
}
