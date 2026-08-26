package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.RestaurantWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantWorkingHoursRepository extends JpaRepository<RestaurantWorkingHours, Long> {
    void deleteByRestaurantId(Long restaurantId); // when editing hours, just going to delete and recreate
    List<RestaurantWorkingHours> findByRestaurantId(Long restaurantId);
}
