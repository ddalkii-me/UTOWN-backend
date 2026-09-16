package com.utown.utownbackend.service;

public interface SmsService {

    void sendVerificationCode(String phone, String code);
}
