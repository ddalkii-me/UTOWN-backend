package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import com.utown.utownbackend.repository.RestaurantWorkingHoursRepository;
import com.utown.utownbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.entity.User;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantTypeRepository restaurantTypeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RestaurantWorkingHoursRepository workingHoursRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Test
    void createRestaurant_deletedCity_throwsNotFound() {

        Long cityId = 1L;

        RestaurantRequestDto request = new RestaurantRequestDto(
                1L,
                1L,
                cityId,
                "Test Restaurant",
                "Test description",
                "Test address",
                "01012345678",
                null,
                null,
                null,
                null,
                com.utown.utownbackend.entity.RestaurantStatus.OPEN
        );

        when(userRepository.findById(request.ownerId()))
                .thenReturn(java.util.Optional.of(new com.utown.utownbackend.entity.User()));

        when(restaurantTypeRepository.findById(request.typeId()))
                .thenReturn(java.util.Optional.of(
                        new com.utown.utownbackend.entity.RestaurantType()
                ));

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(java.util.Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantService.createRestaurant(request)
        );
    }
    @Test
    void updateRestaurant_deletedCity_throwsNotFound() {

        Long restaurantId = 1L;
        Long cityId = 1L;

        RestaurantRequestDto request = new RestaurantRequestDto(
                1L,
                1L,
                cityId,
                "Updated Restaurant",
                "Updated description",
                "Updated address",
                "01012345678",
                null,
                null,
                null,
                null,
                RestaurantStatus.OPEN
        );

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);

        when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant));

        when(userRepository.findById(request.ownerId()))
                .thenReturn(Optional.of(new User()));

        when(restaurantTypeRepository.findById(request.typeId()))
                .thenReturn(Optional.of(new RestaurantType()));

        when(cityRepository.findByIdAndDeletedAtIsNull(cityId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantService.updateRestaurant(
                        restaurantId,
                        request
                )
        );
    }
}