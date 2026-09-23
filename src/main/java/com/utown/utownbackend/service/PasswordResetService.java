package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.PasswordResetConfirmDto;
import com.utown.utownbackend.dto.PasswordResetRequestDto;
import com.utown.utownbackend.dto.PasswordResetRequestResponseDto;
import com.utown.utownbackend.dto.PasswordResetVerifyDto;
import com.utown.utownbackend.dto.PasswordResetVerifyResponseDto;

public interface PasswordResetService {

    PasswordResetRequestResponseDto requestPasswordReset(PasswordResetRequestDto request);

    PasswordResetVerifyResponseDto verifyPasswordReset(PasswordResetVerifyDto request);

    void confirmPasswordReset(PasswordResetConfirmDto request);
}
