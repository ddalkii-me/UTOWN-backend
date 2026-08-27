package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;

    @Override
    public CityResponseDto createCity(CityRequestDto request) {

        City city = new City();

        city.setName(request.name());

        City savedCity = cityRepository.save(city);

        return toDto(savedCity);
    }

    @Override
    public List<CityResponseDto> getAllCities() {

        List<City> cities = cityRepository.findAll();

        return cities.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CityResponseDto getCityById(Long id) {

        City city = cityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        return toDto(city);
    }

    @Override
    public CityResponseDto updateCity(
            Long id,
            CityRequestDto request) {

        City city = cityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        city.setName(request.name());

        City updatedCity = cityRepository.save(city);

        return toDto(updatedCity);
    }
    @Override
    public void deleteCity(Long id) {

        City city = cityRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        cityRepository.delete(city);
    }
    private CityResponseDto toDto(City city) {

        return new CityResponseDto(
                city.getId(),
                city.getName()
        );
    }
}