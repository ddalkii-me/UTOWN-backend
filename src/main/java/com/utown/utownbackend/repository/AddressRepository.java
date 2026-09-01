package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findAllByDeletedAtIsNull();

    List<Address> findAllByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Address> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByDeliveryAreaIdAndDeletedAtIsNull(Long deliveryAreaId);
}