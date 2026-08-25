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

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new RuntimeException("Restaurant type not found"));

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        Restaurant restaurant = new Restaurant();

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setMinimumOrderAmount(request.minimumOrderAmount());
        restaurant.setStatus(request.status());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toDto(savedRestaurant);
    }

    @Override
    public List<RestaurantResponseDto> getAllRestaurants() {

        List<Restaurant> restaurants =
                restaurantRepository.findAllByDeletedAtIsNull();

        return restaurants.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RestaurantResponseDto getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        return toDto(restaurant);
    }

    @Override
    public RestaurantResponseDto updateRestaurant(
            Long id,
            RestaurantRequestDto request) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new RuntimeException("Restaurant type not found"));

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new RuntimeException("City not found"));

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setMinimumOrderAmount(request.minimumOrderAmount());
        restaurant.setStatus(request.status());

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        return toDto(updatedRestaurant);
    }

    @Override
    public void deleteRestaurant(Long id) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        restaurant.setDeletedAt(LocalDateTime.now());

        restaurantRepository.save(restaurant);
    }

    private RestaurantResponseDto toDto(Restaurant restaurant) {
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
}