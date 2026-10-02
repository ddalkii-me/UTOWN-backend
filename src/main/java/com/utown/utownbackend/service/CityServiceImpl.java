package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.utown.utownbackend.exception.ResourceConflictException;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public CityResponseDto createCity(CityRequestDto request) {
        log.info("Executing createCity");

        if (cityRepository.existsByName(request.name())) {
            throw new ResourceConflictException(
                    "City with this name already exists."
            );
        }

        City city = new City();

        city.setName(request.name());

        City savedCity = cityRepository.save(city);

        return toDto(savedCity);
    }

    @Override
    public List<CityResponseDto> getAllCities() {
        log.info("Executing getAllCities");

        List<City> cities = cityRepository.findAllByDeletedAtIsNull();

        return cities.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CityResponseDto getCityById(Long id) {
        log.info("Executing getCityById with id={}", id);

        City city = cityRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        return toDto(city);
    }

    @Override
    public CityResponseDto updateCity(
            Long id,
            CityRequestDto request) {
        log.info("Executing updateCity with id={}", id);

        City city = cityRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        if (cityRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResourceConflictException(
                    "City with this name already exists."
            );
        }

        city.setName(request.name());

        City updatedCity = cityRepository.save(city);

        return toDto(updatedCity);
    }
    @Transactional
    @Override
    public void deleteCity(Long id) {
        log.info("Executing deleteCity with id={}", id);

        City city = cityRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        if (restaurantRepository.existsByCityIdAndDeletedAtIsNull(id)) {
            throw new ResourceConflictException(
                    "Cannot delete city because it still has active restaurants."
            );
        }

        city.setDeletedAt(LocalDateTime.now());

        cityRepository.save(city);
    }
    private CityResponseDto toDto(City city) {

        return new CityResponseDto(
                city.getId(),
                city.getName()
        );
    }
}