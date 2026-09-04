package com.utown.utownbackend.controller;

import com.utown.utownbackend.dto.*;
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

    private OrderResponseDto createDummyOrderResponse(Long id, OrderStatus status) {
        return new OrderResponseDto(
                id,
                1L,
                10L,
                100L,
                "ORD-12345",
                status,
                PaymentMethod.CARD,
                PaymentStatus.PENDING,
                "Leave at door",
                status == OrderStatus.ACCEPTED ? 30 : null,
                status == OrderStatus.DECLINED ? "Too busy" : null,
                BigDecimal.valueOf(25.00),
                BigDecimal.ZERO,
                BigDecimal.valueOf(25.00),
                "USD",
                status == OrderStatus.ACCEPTED ? LocalDateTime.now() : null,
                status == OrderStatus.DECLINED ? LocalDateTime.now() : null,
                null,
                null,
                status == OrderStatus.COMPLETED ? LocalDateTime.now() : null,
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

        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.PENDING);
        when(orderService.createOrder(request)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.createOrder(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        assertEquals("ORD-12345", response.getBody().orderNumber());
        verify(orderService).createOrder(request);
    }

    @Test
    void getOrders_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.PENDING);
        when(orderService.getOrders(10L, 1L, List.of(OrderStatus.PENDING))).thenReturn(List.of(mockResponse));

        ResponseEntity<List<OrderResponseDto>> response = orderController.getOrders(10L, 1L, List.of(OrderStatus.PENDING));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(orderService).getOrders(10L, 1L, List.of(OrderStatus.PENDING));
    }

    @Test
    void getOrders_withoutFilters_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.PENDING);
        when(orderService.getOrders(null, null, null)).thenReturn(List.of(mockResponse));

        ResponseEntity<List<OrderResponseDto>> response = orderController.getOrders(null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(orderService).getOrders(null, null, null);
    }

    @Test
    void getOrderById_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.PENDING);
        when(orderService.getOrderById(1L)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.getOrderById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().id());
        verify(orderService).getOrderById(1L);
    }

    @Test
    void acceptOrder_returnsOk() {
        OrderAcceptRequestDto request = new OrderAcceptRequestDto(30);
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.ACCEPTED);
        when(orderService.acceptOrder(1L, request)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.acceptOrder(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.ACCEPTED, response.getBody().status());
        assertEquals(30, response.getBody().estimatedCookingMinutes());
        verify(orderService).acceptOrder(1L, request);
    }

    @Test
    void startPreparation_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.IN_PREPARATION);
        when(orderService.startPreparation(1L)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.startPreparation(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.IN_PREPARATION, response.getBody().status());
        verify(orderService).startPreparation(1L);
    }

    @Test
    void completeOrder_returnsOk() {
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.COMPLETED);
        when(orderService.completeOrder(1L)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.completeOrder(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.COMPLETED, response.getBody().status());
        verify(orderService).completeOrder(1L);
    }

    @Test
    void declineOrder_returnsOk() {
        OrderDeclineRequestDto request = new OrderDeclineRequestDto("Kitchen is closing");
        OrderResponseDto mockResponse = createDummyOrderResponse(1L, OrderStatus.DECLINED);
        when(orderService.declineOrder(1L, request)).thenReturn(mockResponse);

        ResponseEntity<OrderResponseDto> response = orderController.declineOrder(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(OrderStatus.DECLINED, response.getBody().status());
        assertEquals("Too busy", response.getBody().rejectionReason());
        verify(orderService).declineOrder(1L, request);
    }
}
