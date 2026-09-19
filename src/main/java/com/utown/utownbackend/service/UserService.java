package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.ChangePasswordRequestDto;
import com.utown.utownbackend.dto.UserProfileResponseDto;
import com.utown.utownbackend.dto.UserProfileUpdateRequestDto;
import com.utown.utownbackend.dto.UserStatusUpdateRequestDto;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;

import java.util.List;

public interface UserService {

    UserProfileResponseDto getCurrentUserProfile(String phone);

    UserProfileResponseDto updateCurrentUserProfile(String phone, UserProfileUpdateRequestDto request);

    void changePassword(String phone, ChangePasswordRequestDto request);

    void deleteCurrentUser(String phone);

    UserProfileResponseDto getUserById(Long id);

    List<UserProfileResponseDto> getUsers(UserRole role, UserStatus status);

    UserProfileResponseDto updateUserStatus(Long id, UserStatusUpdateRequestDto request);
}
