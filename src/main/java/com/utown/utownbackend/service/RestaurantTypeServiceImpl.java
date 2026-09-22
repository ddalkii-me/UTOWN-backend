package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantTypeServiceImpl implements RestaurantTypeService {

    private final RestaurantTypeRepository restaurantTypeRepository;

    @Override
    public RestaurantTypeResponseDto createRestaurantType(
            RestaurantTypeRequestDto request) {
        log.info("Executing createRestaurantType");

        RestaurantType restaurantType = new RestaurantType();

        restaurantType.setName(request.name());
        restaurantType.setDescription(request.description());

        RestaurantType savedRestaurantType =
                restaurantTypeRepository.save(restaurantType);

        return toDto(savedRestaurantType);
    }

    @Override
    public List<RestaurantTypeResponseDto> getAllRestaurantTypes() {
        log.info("Executing getAllRestaurantTypes");

        List<RestaurantType> restaurantTypes =
                restaurantTypeRepository.findAll();

        return restaurantTypes.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RestaurantTypeResponseDto getRestaurantTypeById(Long id) {
        log.info("Executing getRestaurantTypeById with id={}", id);

        RestaurantType restaurantType = restaurantTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        return toDto(restaurantType);
    }

    @Override
    public RestaurantTypeResponseDto updateRestaurantType(
            Long id,
            RestaurantTypeRequestDto request) {
        log.info("Executing updateRestaurantType with id={}", id);

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
        log.info("Executing deleteRestaurantType with id={}", id);

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