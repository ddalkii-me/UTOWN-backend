package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.RestaurantDeliveryArea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantDeliveryAreaRepository
        extends JpaRepository<RestaurantDeliveryArea, Long> {

    @EntityGraph(attributePaths = {"restaurant", "deliveryArea"})
    List<RestaurantDeliveryArea> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"restaurant", "deliveryArea"})
    List<RestaurantDeliveryArea> findAllByRestaurantIdAndDeletedAtIsNull(Long restaurantId);

    @EntityGraph(attributePaths = {"restaurant", "deliveryArea"})
    Optional<RestaurantDeliveryArea> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"restaurant", "deliveryArea"})
    Optional<RestaurantDeliveryArea>
    findByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
            Long restaurantId,
            Long deliveryAreaId
    );

    Optional<RestaurantDeliveryArea>
    findByRestaurantIdAndDeliveryAreaId(
            Long restaurantId,
            Long deliveryAreaId
    );
}