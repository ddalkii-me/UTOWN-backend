package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DeliveryArea;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, Long> {

    @EntityGraph(attributePaths = {"city"})
    List<DeliveryArea> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"city"})
    List<DeliveryArea> findAllByCityIdAndDeletedAtIsNull(Long cityId);

    @EntityGraph(attributePaths = {"city"})
    Optional<DeliveryArea> findByIdAndDeletedAtIsNull(Long id);

    //will be use in creating addressServiceImpl
    @EntityGraph(attributePaths = {"city"})
    Optional<DeliveryArea> findByIdAndCityIdAndDeletedAtIsNull(
            Long id,
            Long cityId
    );
    boolean existsByCityIdAndNameAndDeletedAtIsNull(
            Long cityId,
            String name
    );
    boolean existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(
            Long cityId,
            String name,
            Long id
    );
}