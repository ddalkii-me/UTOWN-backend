package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.PaymentResult;
import com.utown.utownbackend.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentGatewayImplTest {

    private MockPaymentGatewayImpl gateway;
    private Order order;

    @BeforeEach
    void setUp() {
        gateway = new MockPaymentGatewayImpl();
        order = new Order();
        order.setId(100L);
    }

    @Test
    @DisplayName("charge - normal amount succeeds with transaction ID")
    void charge_success() {
        PaymentResult result = gateway.charge(order, BigDecimal.valueOf(25.00));
        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("txn_mock_"));
        assertEquals("Success", result.message());
    }

    @Test
    @DisplayName("charge - amount 9999.99 fails")
    void charge_fixedFailureAmount() {
        PaymentResult result = gateway.charge(order, new BigDecimal("9999.99"));
        assertFalse(result.success());
        assertNull(result.transactionId());
        assertEquals("Insufficient funds", result.message());
    }

    @Test
    @DisplayName("charge - forceChargeFailure allows configuring test failure")
    void charge_forceChargeFailure() {
        gateway.setForceChargeFailure(true);
        gateway.setFailureReason("Card expired");

        PaymentResult result = gateway.charge(order, BigDecimal.valueOf(50.00));
        assertFalse(result.success());
        assertNull(result.transactionId());
        assertEquals("Card expired", result.message());

        gateway.reset();
        PaymentResult resetResult = gateway.charge(order, BigDecimal.valueOf(50.00));
        assertTrue(resetResult.success());
    }

    @Test
    @DisplayName("refund - valid transaction ID succeeds")
    void refund_success() {
        PaymentResult result = gateway.refund(order, "txn_mock_123");
        assertTrue(result.success());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("ref_mock_"));
    }

    @Test
    @DisplayName("refund - missing transaction ID fails")
    void refund_missingTransactionId() {
        PaymentResult result = gateway.refund(order, null);
        assertFalse(result.success());
        assertEquals("No transaction ID provided", result.message());
    }

    @Test
    @DisplayName("refund - forceRefundFailure allows configuring test failure")
    void refund_forceRefundFailure() {
        gateway.setForceRefundFailure(true);
        gateway.setFailureReason("Refund window expired");

        PaymentResult result = gateway.refund(order, "txn_mock_123");
        assertFalse(result.success());
        assertEquals("Refund window expired", result.message());

        gateway.reset();
        PaymentResult resetResult = gateway.refund(order, "txn_mock_123");
        assertTrue(resetResult.success());
    }
}
