package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto request);
    List<OrderResponseDto> getAllOrders();
    OrderResponseDto getOrderById(Long id);
}

