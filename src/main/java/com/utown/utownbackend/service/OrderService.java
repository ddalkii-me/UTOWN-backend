package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.OrderAcceptRequestDto;
import com.utown.utownbackend.dto.OrderDeclineRequestDto;
import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;
import com.utown.utownbackend.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto request);
    List<OrderResponseDto> getAllOrders();
    List<OrderResponseDto> getOrders(Long restaurantId, Long userId, List<OrderStatus> statuses);
    OrderResponseDto getOrderById(Long id);
    OrderResponseDto acceptOrder(Long id, OrderAcceptRequestDto request);
    OrderResponseDto startPreparation(Long id);
    OrderResponseDto completeOrder(Long id);
    OrderResponseDto declineOrder(Long id, OrderDeclineRequestDto request);
}


