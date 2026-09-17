package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.OrderAcceptRequestDto;
import com.utown.utownbackend.dto.OrderDeclineRequestDto;
import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;
import com.utown.utownbackend.entity.OrderStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface OrderService {
    OrderResponseDto createOrder(OrderRequestDto request);
    List<OrderResponseDto> getOrders(Long restaurantId,
                                     Long userId,
                                     List<OrderStatus> statuses,
                                     Authentication authentication);
    OrderResponseDto getOrderById(Long id);
    OrderResponseDto acceptOrder(Long id, OrderAcceptRequestDto request);
    OrderResponseDto startPreparation(Long id);
    OrderResponseDto completeOrder(Long id);
    OrderResponseDto declineOrder(Long id, OrderDeclineRequestDto request);
}


