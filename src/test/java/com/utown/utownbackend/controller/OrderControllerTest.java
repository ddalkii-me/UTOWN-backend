package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.OrderItemResponseDto;
import com.utown.utownbackend.dto.OrderRequestDto;
import com.utown.utownbackend.dto.OrderResponseDto;
import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.entity.PaymentMethod;
import com.utown.utownbackend.entity.PaymentStatus;
import com.utown.utownbackend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderResponseDto createDummyOrderResponse(Long id) {
        return new OrderResponseDto(
                id,
                1L,
                10L,
                100L,
                "ORD-12345",
                OrderStatus.PENDING,
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                "Leave at door",
                null,
                null,
                BigDecimal.valueOf(25.00),
                BigDecimal.ZERO,
                BigDecimal.valueOf(25.00),
                "USD",
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    @Test
    void createOrder_returnsCreatedStatusAndBody() {
        OrderRequestDto request = new OrderRequestDto(
                1L,
                10L,
                100L,
                "Leave at door",
                PaymentMethod.CARD,
                List.of()
        );

        OrderResponseDto mockResponse = createDummyOrderResponse(1L);
        when(orderService.createOrder(request)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.createOrder(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("ORD-12345", response.getBody().orderNumber());
        verify(orderService).createOrder(request);
    }

    @Test
    void getAllOrders_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L);
        when(orderService.getAllOrders()).thenReturn(List.of(mockResponse));

        ResponseEntity<List<OrderResponseDto>> response = orderController.getAllOrders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(orderService).getAllOrders();
    }

    @Test
    void getOrderById_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L);
        when(orderService.getOrderById(1L)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.getOrderById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(orderService).getOrderById(1L);
    }
}
