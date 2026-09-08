package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findAllByOrderId(Long orderId);
    List<OrderStatusHistory> findAllByOrderIdIn(List<Long> orderIds);
}
