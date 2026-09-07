package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantDeliveryAreaResponseDto;
import com.utown.utownbackend.entity.DeliveryArea;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantDeliveryArea;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.DeliveryAreaRepository;
import com.utown.utownbackend.repository.RestaurantDeliveryAreaRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantDeliveryAreaServiceImpl
        implements RestaurantDeliveryAreaService {

    private final RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;

    @Override
    public RestaurantDeliveryAreaResponseDto addDeliveryArea(
            Long restaurantId,
            Long deliveryAreaId
    ) {

        Restaurant restaurant = restaurantRepository
                .findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Restaurant not found with id: " + restaurantId
                        )
                );

        DeliveryArea deliveryArea = deliveryAreaRepository
                .findByIdAndDeletedAtIsNull(deliveryAreaId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Delivery area not found with id: " + deliveryAreaId
                        )
                );

        if (restaurant.getCity() == null
                || restaurant.getCity().getId() == null
                || deliveryArea.getCity() == null
                || deliveryArea.getCity().getId() == null
                || !restaurant.getCity().getId()
                .equals(deliveryArea.getCity().getId())) {

            throw new ResourceConflictException(
                    "Restaurant and delivery area must belong to the same city."
            );
        }

        RestaurantDeliveryArea existing =
                restaurantDeliveryAreaRepository
                        .findByRestaurantIdAndDeliveryAreaId(
                                restaurantId,
                                deliveryAreaId
                        )
                        .orElse(null);

        if (existing != null) {

            if (existing.getDeletedAt() != null) {

                existing.setDeletedAt(null);

                RestaurantDeliveryArea restored =
                        restaurantDeliveryAreaRepository.save(existing);

                return toResponseDto(restored);
            }

            throw new ResourceConflictException(
                    "Restaurant already serves this delivery area"
            );
        }

        RestaurantDeliveryArea restaurantDeliveryArea =
                new RestaurantDeliveryArea();

        restaurantDeliveryArea.setRestaurant(restaurant);
        restaurantDeliveryArea.setDeliveryArea(deliveryArea);

        RestaurantDeliveryArea saved =
                restaurantDeliveryAreaRepository.save(
                        restaurantDeliveryArea
                );

        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDeliveryAreaResponseDto>
    getDeliveryAreasByRestaurant(Long restaurantId) {

        restaurantRepository
                .findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Restaurant not found with id: " + restaurantId
                        )
                );

        return restaurantDeliveryAreaRepository
                .findAllByRestaurantIdAndDeletedAtIsNull(restaurantId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public void removeDeliveryArea(
            Long restaurantId,
            Long deliveryAreaId
    ) {

        RestaurantDeliveryArea restaurantDeliveryArea =
                restaurantDeliveryAreaRepository
                        .findByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
                                restaurantId,
                                deliveryAreaId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Restaurant " + restaurantId
                                                + " does not serve delivery area "
                                                + deliveryAreaId
                                )
                        );

        restaurantDeliveryArea.setDeletedAt(
                LocalDateTime.now()
        );

        restaurantDeliveryAreaRepository.save(
                restaurantDeliveryArea
        );
    }

    private RestaurantDeliveryAreaResponseDto toResponseDto(
            RestaurantDeliveryArea restaurantDeliveryArea
    ) {
        return new RestaurantDeliveryAreaResponseDto(
                restaurantDeliveryArea.getId(),
                restaurantDeliveryArea.getRestaurant().getId(),
                restaurantDeliveryArea.getDeliveryArea().getId(),
                restaurantDeliveryArea.getDeliveryArea().getName()
        );
    }
}