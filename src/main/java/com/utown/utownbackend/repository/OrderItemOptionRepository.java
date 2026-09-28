package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.OrderItemOption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemOptionRepository extends JpaRepository<OrderItemOption, Long> {

    @EntityGraph(attributePaths = {"orderItem"})
    List<OrderItemOption> findAllByOrderItemId(Long orderItemId);

    @EntityGraph(attributePaths = {"orderItem"})
    List<OrderItemOption> findAllByOrderItemIdIn(List<Long> orderItemIds);
}

