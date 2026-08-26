package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantTypeServiceImpl implements RestaurantTypeService {

    private final RestaurantTypeRepository restaurantTypeRepository;

    @Override
    public RestaurantTypeResponseDto createRestaurantType(
            RestaurantTypeRequestDto request) {

        RestaurantType restaurantType = new RestaurantType();

        restaurantType.setName(request.name());
        restaurantType.setDescription(request.description());

        RestaurantType savedRestaurantType =
                restaurantTypeRepository.save(restaurantType);

        return toDto(savedRestaurantType);
    }

    @Override
    public List<RestaurantTypeResponseDto> getAllRestaurantTypes() {

        List<RestaurantType> restaurantTypes =
                restaurantTypeRepository.findAll();

        return restaurantTypes.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RestaurantTypeResponseDto getRestaurantTypeById(Long id) {

        RestaurantType restaurantType = restaurantTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        return toDto(restaurantType);
    }

    @Override
    public RestaurantTypeResponseDto updateRestaurantType(
            Long id,
            RestaurantTypeRequestDto request) {

        RestaurantType restaurantType = restaurantTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        restaurantType.setName(request.name());
        restaurantType.setDescription(request.description());

        RestaurantType updatedRestaurantType =
                restaurantTypeRepository.save(restaurantType);

        return toDto(updatedRestaurantType);
    }

    @Override
    public void deleteRestaurantType(Long id) {

        RestaurantType restaurantType = restaurantTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        restaurantTypeRepository.delete(restaurantType);
    }

    private RestaurantTypeResponseDto toDto(RestaurantType restaurantType) {
        return new RestaurantTypeResponseDto(
                restaurantType.getId(),
                restaurantType.getName(),
                restaurantType.getDescription()
        );
    }

}