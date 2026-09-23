package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = {"user", "restaurant", "address"})
    Optional<Order> findByIdWithDetails(Long id);

    @EntityGraph(attributePaths = {
            "restaurant",
            "restaurant.owner",
            "user"
    })
    Optional<Order> findWithUserAndRestaurantOwnerById(Long id);

}

