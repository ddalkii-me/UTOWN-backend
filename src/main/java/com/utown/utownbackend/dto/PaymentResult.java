package com.utown.utownbackend.dto;

public record PaymentResult(
        boolean success,
        String transactionId,
        String message
) {}
