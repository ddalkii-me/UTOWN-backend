package com.utown.utownbackend.service;

import com.utown.utownbackend.entity.Order;
import com.utown.utownbackend.dto.PaymentResult;
import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentResult charge(Order order, BigDecimal amount);
    PaymentResult refund(Order order, String transactionId);
}
