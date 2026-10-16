package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.PaymentResult;
import com.utown.utownbackend.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class MockPaymentGatewayImpl implements PaymentGateway {

    private volatile boolean forceChargeFailure = false;
    private volatile boolean forceRefundFailure = false;
    private volatile String failureReason = "Payment rejected";

    public void setForceChargeFailure(boolean forceChargeFailure) {
        this.forceChargeFailure = forceChargeFailure;
    }

    public void setForceRefundFailure(boolean forceRefundFailure) {
        this.forceRefundFailure = forceRefundFailure;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void reset() {
        this.forceChargeFailure = false;
        this.forceRefundFailure = false;
        this.failureReason = "Payment rejected";
    }

    @Override
    public PaymentResult charge(Order order, BigDecimal amount) {
        log.info("MockPaymentGateway: processing charge of {} for order {}", amount, order != null ? order.getId() : null);
        
        if (forceChargeFailure) {
            log.warn("MockPaymentGateway: Charge failed for order {}", order != null ? order.getId() : null);
            return new PaymentResult(false, null, failureReason != null ? failureReason : "Payment rejected");
        }

        if (amount != null && amount.compareTo(new BigDecimal("9999.99")) == 0) {
            log.warn("MockPaymentGateway: Charge failed for order {}", order != null ? order.getId() : null);
            return new PaymentResult(false, null, "Insufficient funds");
        }
        
        String transactionId = "txn_mock_" + UUID.randomUUID().toString();
        log.info("MockPaymentGateway: Charge successful for order {}. Transaction ID: {}", order != null ? order.getId() : null, transactionId);
        return new PaymentResult(true, transactionId, "Success");
    }

    @Override
    public PaymentResult refund(Order order, String transactionId) {
        log.info("MockPaymentGateway: processing refund for transaction {} of order {}", transactionId, order != null ? order.getId() : null);
        
        if (forceRefundFailure) {
            log.warn("MockPaymentGateway: Refund forced failure for order {}", order != null ? order.getId() : null);
            return new PaymentResult(false, null, failureReason != null ? failureReason : "Refund rejected");
        }

        if (transactionId == null || transactionId.isEmpty()) {
            return new PaymentResult(false, null, "No transaction ID provided");
        }
        
        String refundTxnId = "ref_mock_" + UUID.randomUUID().toString();
        log.info("MockPaymentGateway: Refund successful for order {}. Refund Transaction ID: {}", order != null ? order.getId() : null, refundTxnId);
        return new PaymentResult(true, refundTxnId, "Refund Success");
    }
}
