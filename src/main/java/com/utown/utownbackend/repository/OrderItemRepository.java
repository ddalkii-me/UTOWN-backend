package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.OrderItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @EntityGraph(attributePaths = {"dish"})
    List<OrderItem> findAllByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"dish"})
    List<OrderItem> findAllByOrderIdIn(List<Long> orderIds);
}

