package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import com.utown.utownbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantTypeRepository restaurantTypeRepository;
    private final CityRepository cityRepository;

    @Override
    public RestaurantResponseDto createRestaurant(RestaurantRequestDto request) {

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new RuntimeException("Restaurant type not found"));

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        Restaurant restaurant = new Restaurant();

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setMinimumOrderAmount(request.getMinimumOrderAmount());
        restaurant.setStatus(request.getStatus());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return new RestaurantResponseDto(
                savedRestaurant.getId(),
                savedRestaurant.getOwner().getId(),
                savedRestaurant.getType().getId(),
                savedRestaurant.getCity().getId(),
                savedRestaurant.getName(),
                savedRestaurant.getDescription(),
                savedRestaurant.getAddress(),
                savedRestaurant.getPhone(),
                savedRestaurant.getLogoUrl(),
                savedRestaurant.getLatitude(),
                savedRestaurant.getLongitude(),
                savedRestaurant.getMinimumOrderAmount(),
                savedRestaurant.getStatus()
        );
    }

    @Override
    public List<RestaurantResponseDto> getAllRestaurants() {

        List<Restaurant> restaurants =
                restaurantRepository.findAllByDeletedAtIsNull();

        return restaurants.stream()
                .map(restaurant -> new RestaurantResponseDto(
                        restaurant.getId(),
                        restaurant.getOwner().getId(),
                        restaurant.getType().getId(),
                        restaurant.getCity().getId(),
                        restaurant.getName(),
                        restaurant.getDescription(),
                        restaurant.getAddress(),
                        restaurant.getPhone(),
                        restaurant.getLogoUrl(),
                        restaurant.getLatitude(),
                        restaurant.getLongitude(),
                        restaurant.getMinimumOrderAmount(),
                        restaurant.getStatus()
                ))
                .toList();
    }

    @Override
    public RestaurantResponseDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return new RestaurantResponseDto(
                restaurant.getId(),
                restaurant.getOwner().getId(),
                restaurant.getType().getId(),
                restaurant.getCity().getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getLogoUrl(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getMinimumOrderAmount(),
                restaurant.getStatus()
        );
    }

    @Override
    public RestaurantResponseDto updateRestaurant(
            Long id,
            RestaurantRequestDto request) {

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new RuntimeException("Restaurant type not found"));

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.getName());
        restaurant.setDescription(request.getDescription());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setLogoUrl(request.getLogoUrl());
        restaurant.setLatitude(request.getLatitude());
        restaurant.setLongitude(request.getLongitude());
        restaurant.setMinimumOrderAmount(request.getMinimumOrderAmount());
        restaurant.setStatus(request.getStatus());

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        return new RestaurantResponseDto(
                updatedRestaurant.getId(),
                updatedRestaurant.getOwner().getId(),
                updatedRestaurant.getType().getId(),
                updatedRestaurant.getCity().getId(),
                updatedRestaurant.getName(),
                updatedRestaurant.getDescription(),
                updatedRestaurant.getAddress(),
                updatedRestaurant.getPhone(),
                updatedRestaurant.getLogoUrl(),
                updatedRestaurant.getLatitude(),
                updatedRestaurant.getLongitude(),
                updatedRestaurant.getMinimumOrderAmount(),
                updatedRestaurant.getStatus()
        );
    }

    @Override
    public void deleteRestaurant(Long id) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        restaurant.setDeletedAt(LocalDateTime.now());

        restaurantRepository.save(restaurant);
    }
}