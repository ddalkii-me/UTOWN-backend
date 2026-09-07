package com.utown.utownbackend.service;

import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.exception.ResourceConflictException;

import com.utown.utownbackend.util.TestDataFactory;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceImplTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private CityServiceImpl cityService;

    private City city;
    private CityRequestDto requestDto;

    @BeforeEach
    void setUp() {
        city = TestDataFactory.createCity(1L, "Seoul");
        requestDto = new CityRequestDto("Seoul");
    }

    // ── createCity ───────────────────────────────────────────────────

    @Test
    @DisplayName("createCity - should create and return city")
    void createCity_success() {
        CityRequestDto createRequest = new CityRequestDto("Busan");

        City savedCity = new City();
        savedCity.setId(2L);
        savedCity.setName("Busan");

        when(cityRepository.existsByName("Busan")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenReturn(savedCity);

        CityResponseDto result = cityService.createCity(createRequest);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Busan");
        verify(cityRepository).existsByName("Busan");
        verify(cityRepository).save(any(City.class));
    }

    @Test
    @DisplayName("createCity - should throw conflict when name is duplicate")
    void createCity_duplicateName_throwsConflict() {
        when(cityRepository.existsByName("Seoul")).thenReturn(true);

        assertThatThrownBy(() -> cityService.createCity(requestDto))
                .isInstanceOf(ResourceConflictException.class);
        verify(cityRepository).existsByName("Seoul");
        verify(cityRepository, never()).save(any(City.class));
    }

    // ── getAllCities ──────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCities - should return list of active cities")
    void getAllCities_success() {
        City city2 = new City();
        city2.setId(2L);
        city2.setName("Busan");

        when(cityRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(city, city2));

        List<CityResponseDto> result = cityService.getAllCities();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Seoul");
        assertThat(result.get(1).id()).isEqualTo(2L);
        assertThat(result.get(1).name()).isEqualTo("Busan");
        verify(cityRepository).findAllByDeletedAtIsNull();
    }

    // ── getCityById ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCityById - should return city when found")
    void getCityById_success() {
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));

        CityResponseDto result = cityService.getCityById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Seoul");
        verify(cityRepository).findByIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("getCityById - should throw when city not found")
    void getCityById_notFound_throwsNotFound() {
        when(cityRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getCityById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateCity ───────────────────────────────────────────────────

    @Test
    @DisplayName("updateCity - should update and return city")
    void updateCity_success() {
        CityRequestDto updateRequest = new CityRequestDto("Busan");

        City updatedCity = new City();
        updatedCity.setId(1L);
        updatedCity.setName("Busan");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(cityRepository.existsByNameAndIdNot("Busan", 1L)).thenReturn(false);
        when(cityRepository.save(city)).thenReturn(updatedCity);

        CityResponseDto result = cityService.updateCity(1L, updateRequest);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Busan");
        verify(cityRepository).existsByNameAndIdNot("Busan", 1L);
        verify(cityRepository).save(city);
    }

    @Test
    @DisplayName("updateCity - should throw when city not found")
    void updateCity_notFound_throwsNotFound() {
        CityRequestDto updateRequest = new CityRequestDto("Busan");

        when(cityRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.updateCity(99L, updateRequest))
                .isInstanceOf(EntityNotFoundException.class);
        verify(cityRepository, never()).save(any(City.class));
    }

    @Test
    @DisplayName("updateCity - should throw conflict when name is duplicate")
    void updateCity_duplicateName_throwsConflict() {
        CityRequestDto updateRequest = new CityRequestDto("Busan");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(cityRepository.existsByNameAndIdNot("Busan", 1L)).thenReturn(true);

        assertThatThrownBy(() -> cityService.updateCity(1L, updateRequest))
                .isInstanceOf(ResourceConflictException.class);
        verify(cityRepository, never()).save(any(City.class));
    }

    // ── deleteCity ───────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCity - should soft-delete city")
    void deleteCity_success() {
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(restaurantRepository.existsByCityIdAndDeletedAtIsNull(1L)).thenReturn(false);

        cityService.deleteCity(1L);

        assertThat(city.getDeletedAt()).isNotNull();
        verify(cityRepository).save(city);
    }

    @Test
    @DisplayName("deleteCity - should throw conflict when active restaurants exist")
    void deleteCity_withActiveRestaurant_throwsConflict() {
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(restaurantRepository.existsByCityIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> cityService.deleteCity(1L))
                .isInstanceOf(ResourceConflictException.class);
        verify(cityRepository, never()).save(any(City.class));
    }

    @Test
    @DisplayName("deleteCity - should throw when city already deleted or not found")
    void deleteCity_alreadyDeleted_throwsNotFound() {
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.deleteCity(1L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(cityRepository, never()).save(any(City.class));
        verify(restaurantRepository, never()).existsByCityIdAndDeletedAtIsNull(1L);
    }
}