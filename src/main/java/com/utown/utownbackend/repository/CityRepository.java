package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByDeletedAtIsNull();

    Optional<City> findByIdAndDeletedAtIsNull(Long id);

}