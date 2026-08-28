package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.RestaurantWorkingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface RestaurantWorkingHoursRepository extends JpaRepository<RestaurantWorkingHours, Long> {
    Optional<RestaurantWorkingHours> findByRestaurantIdAndDayOfWeek(Long restaurantId, DayOfWeek dayOfWeek);
    List<RestaurantWorkingHours> findByRestaurantId(Long restaurantId);
}
