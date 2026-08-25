package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantTypeRepository extends JpaRepository<RestaurantType, Long> {
}