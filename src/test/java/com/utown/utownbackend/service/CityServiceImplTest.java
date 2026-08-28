package com.utown.utownbackend.service;

import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.entity.City;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.utown.utownbackend.exception.ResourceConflictException;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private CityServiceImpl cityService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createCity_success() {

        CityRequestDto request = new CityRequestDto("Busan");

        City savedCity = new City();
        savedCity.setId(1L);
        savedCity.setName("Busan");

        when(cityRepository.existsByName("Busan"))
                .thenReturn(false);

        when(cityRepository.save(any(City.class)))
                .thenReturn(savedCity);

        CityResponseDto result = cityService.createCity(request);

        assertEquals(1L, result.id());
        assertEquals("Busan", result.name());

        verify(cityRepository).existsByName("Busan");
        verify(cityRepository).save(any(City.class));
    }
    @Test
    void createCity_duplicateName_throwsConflict() {

        CityRequestDto request = new CityRequestDto("Seoul");

        when(cityRepository.existsByName("Seoul"))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> cityService.createCity(request)
        );

        verify(cityRepository).existsByName("Seoul");
        verify(cityRepository, never()).save(any(City.class));
    }
    @Test
    void updateCity_duplicateName_throwsConflict() {

        Long cityId = 2L;

        CityRequestDto request = new CityRequestDto("Seoul");

        City existingCity = new City();
        existingCity.setId(cityId);
        existingCity.setName("Busan");

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.of(existingCity));

        when(cityRepository.existsByNameAndIdNot("Seoul", cityId))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> cityService.updateCity(cityId, request)
        );

        verify(cityRepository)
                .existsByNameAndIdNot("Seoul", cityId);

        verify(cityRepository, never())
                .save(any(City.class));
    }
    @Test
    void updateCity_success() {

        Long cityId = 1L;

        CityRequestDto request = new CityRequestDto("Busan");

        City existingCity = new City();
        existingCity.setId(cityId);
        existingCity.setName("Seoul");

        City updatedCity = new City();
        updatedCity.setId(cityId);
        updatedCity.setName("Busan");

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.of(existingCity));

        when(cityRepository.existsByNameAndIdNot("Busan", cityId))
                .thenReturn(false);

        when(cityRepository.save(existingCity))
                .thenReturn(updatedCity);

        CityResponseDto result =
                cityService.updateCity(cityId, request);

        assertEquals(1L, result.id());
        assertEquals("Busan", result.name());

        verify(cityRepository)
                .existsByNameAndIdNot("Busan", cityId);

        verify(cityRepository).save(existingCity);
    }
    @Test
    void deleteCity_success() {

        Long cityId = 1L;

        City city = new City();
        city.setId(cityId);
        city.setName("Seoul");

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.of(city));

        when(restaurantRepository.existsByCityIdAndDeletedAtIsNull(cityId))
                .thenReturn(false);

        cityService.deleteCity(cityId);

        assertNotNull(city.getDeletedAt());

        verify(cityRepository).save(city);
    }
    @Test
    void deleteCity_withActiveRestaurant_throwsConflict() {

        Long cityId = 1L;

        City city = new City();
        city.setId(cityId);
        city.setName("Seoul");

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.of(city));

        when(restaurantRepository.existsByCityIdAndDeletedAtIsNull(cityId))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> cityService.deleteCity(cityId)
        );

        verify(restaurantRepository)
                .existsByCityIdAndDeletedAtIsNull(cityId);

        verify(cityRepository, never())
                .save(any(City.class));
    }
    @Test
    void deleteCity_alreadyDeleted_throwsNotFound() {

        Long cityId = 1L;

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> cityService.deleteCity(cityId)
        );

        verify(cityRepository)
                .findByIdAndDeletedAtIsNull(cityId);

        verify(cityRepository, never())
                .save(any(City.class));

        verify(
                restaurantRepository,
                never()
        ).existsByCityIdAndDeletedAtIsNull(cityId);
    }
    @Test
    void getAllCities_success() {

        City city1 = new City();
        city1.setId(1L);
        city1.setName("Seoul");

        City city2 = new City();
        city2.setId(2L);
        city2.setName("Busan");

        when(cityRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(city1, city2));

        List<CityResponseDto> result = cityService.getAllCities();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Seoul", result.get(0).name());
        assertEquals(2L, result.get(1).id());
        assertEquals("Busan", result.get(1).name());

        verify(cityRepository).findAllByDeletedAtIsNull();
    }
    @Test
    void getCityById_success() {

        Long cityId = 1L;

        City city = new City();
        city.setId(cityId);
        city.setName("Seoul");

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.of(city));

        CityResponseDto result =
                cityService.getCityById(cityId);

        assertEquals(1L, result.id());
        assertEquals("Seoul", result.name());

        verify(cityRepository)
                .findByIdAndDeletedAtIsNull(cityId);
    }
}