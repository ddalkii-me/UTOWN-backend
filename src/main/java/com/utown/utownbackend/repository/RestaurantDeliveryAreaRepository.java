package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.RestaurantDeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantDeliveryAreaRepository
        extends JpaRepository<RestaurantDeliveryArea, Long> {

    List<RestaurantDeliveryArea> findAllByDeletedAtIsNull();

    List<RestaurantDeliveryArea> findAllByRestaurantIdAndDeletedAtIsNull(Long restaurantId);

    Optional<RestaurantDeliveryArea> findByIdAndDeletedAtIsNull(Long id);

    Optional<RestaurantDeliveryArea>
    findByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
            Long restaurantId,
            Long deliveryAreaId
    );

    boolean existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
            Long restaurantId,
            Long deliveryAreaId
    );

    Optional<RestaurantDeliveryArea>
    findByRestaurantIdAndDeliveryAreaId(
            Long restaurantId,
            Long deliveryAreaId
    );
}