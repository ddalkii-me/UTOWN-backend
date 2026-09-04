package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.OrderItemOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemOptionRepository extends JpaRepository<OrderItemOption, Long> {

    List<OrderItemOption> findAllByOrderItemId(Long orderItemId);

    List<OrderItemOption> findAllByOrderItemIdIn(List<Long> orderItemIds);
}

