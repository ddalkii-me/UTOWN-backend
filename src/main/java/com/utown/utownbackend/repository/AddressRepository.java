package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

    boolean existsByDeliveryAreaIdAndDeletedAtIsNull(Long deliveryAreaId);
}
