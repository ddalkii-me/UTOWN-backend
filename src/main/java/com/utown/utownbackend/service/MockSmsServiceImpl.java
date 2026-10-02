package com.utown.utownbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.info("Executing sendVerificationCode");
        // Mock implementation for development and testing.
        // In production, integrate with a real SMS provider gateway (e.g. Twilio, CoolSMS).
        log.info("[MOCK SMS PROVIDER] Sending verification code to phone: {}", phone);
    }
}
