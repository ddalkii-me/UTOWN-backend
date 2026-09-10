package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.dto.TokenRefreshRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto request);

    AuthResponseDto login(LoginRequestDto request);

    AuthResponseDto refreshToken(TokenRefreshRequestDto request);

    void logout(String phone);
}
