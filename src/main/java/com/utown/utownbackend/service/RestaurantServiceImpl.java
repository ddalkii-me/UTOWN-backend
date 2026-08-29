package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.dto.WorkingHoursDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantTypeRepository restaurantTypeRepository;
    private final CityRepository cityRepository;
    private final RestaurantWorkingHoursRepository workingHoursRepository;

    @Transactional
    @Override
    public RestaurantResponseDto createRestaurant(RestaurantRequestDto request) {

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        City city = cityRepository.findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

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
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        return toDto(restaurant);
    }

    @Transactional
    @Override
    public RestaurantResponseDto updateRestaurant(
            Long id,
            RestaurantRequestDto request) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        City city = cityRepository.findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

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

    @Transactional
    @Override
    public void deleteRestaurant(Long id) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        restaurant.setDeletedAt(LocalDateTime.now());

        restaurantRepository.save(restaurant);
    }

    @Transactional
    @Override
    public void updateWorkingHourForDay(Long restaurantId, DayOfWeek dayOfWeek, WorkingHoursDto dto) {

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        RestaurantWorkingHours entity = workingHoursRepository.findByRestaurantIdAndDayOfWeek(restaurantId, dayOfWeek)
                .orElseGet(RestaurantWorkingHours::new);

        entity.setRestaurant(restaurant);
        entity.setDayOfWeek(dayOfWeek);
        entity.setOpenTime(dto.openTime());
        entity.setCloseTime(dto.closeTime());
        entity.setDayOff(dto.dayOff());

        workingHoursRepository.save(entity);
    }


    @Override
    public List<WorkingHoursDto> getWorkingHours(Long restaurantId) {

        restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        List<RestaurantWorkingHours> entities = workingHoursRepository.findByRestaurantId(restaurantId);

        return entities.stream()
                .map(entity -> new WorkingHoursDto(
                        entity.getDayOfWeek(),
                        entity.getOpenTime(),
                        entity.getCloseTime(),
                        entity.isDayOff()
                ))
                .toList();
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