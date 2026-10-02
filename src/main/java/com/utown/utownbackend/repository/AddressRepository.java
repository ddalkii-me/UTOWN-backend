package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Address;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    @EntityGraph(attributePaths = {"user", "city", "deliveryArea"})
    List<Address> findAllByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"user", "city", "deliveryArea"})
    List<Address> findAllByUserIdAndDeletedAtIsNull(Long userId);

    @EntityGraph(attributePaths = {"user", "city", "deliveryArea"})
    Optional<Address> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByDeliveryAreaIdAndDeletedAtIsNull(Long deliveryAreaId);
}