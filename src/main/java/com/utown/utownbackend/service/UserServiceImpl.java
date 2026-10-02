package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.ChangePasswordRequestDto;
import com.utown.utownbackend.dto.UserProfileResponseDto;
import com.utown.utownbackend.dto.UserProfileUpdateRequestDto;
import com.utown.utownbackend.dto.UserStatusUpdateRequestDto;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getCurrentUserProfile(String phone) {
        log.info("Executing getCurrentUserProfile");
        User user = findActiveUserByPhone(phone);
        return toDto(user);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateCurrentUserProfile(String phone, UserProfileUpdateRequestDto request) {
        log.info("Executing updateCurrentUserProfile");
        User user = findActiveUserByPhone(phone);

        String newEmail = request.email().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail())) {
            userRepository.findByEmailAndDeletedAtIsNull(newEmail).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ResourceConflictException("Email is already in use: " + newEmail);
                }
            });
            user.setEmail(newEmail);
            user.setEmailVerifiedAt(null);
        }

        user.setName(request.name().trim());
        User saved = userRepository.save(user);
        log.info("Updated profile for user ID: {}", saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void changePassword(String phone, ChangePasswordRequestDto request) {
        log.info("Executing changePassword");
        User user = findActiveUserByPhone(phone);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user.getId());
        log.info("Changed password and invalidated tokens for user ID: {}", user.getId());
    }

    @Override
    @Transactional
    public void deleteCurrentUser(String phone) {
        log.info("Executing deleteCurrentUser");
        User user = findActiveUserByPhone(phone);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenService.deleteByUserId(user.getId());
        log.info("Soft-deleted user ID: {}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getUserById(Long id) {
        log.info("Executing getUserById with id={}", id);
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponseDto> getUsers(UserRole role, UserStatus status) {
        log.info("Executing getUsers with role={}, status={}", role, status);
        List<User> users;
        if (role != null && status != null) {
            users = userRepository.findByRoleAndStatusAndDeletedAtIsNull(role, status);
        } else if (role != null) {
            users = userRepository.findByRoleAndDeletedAtIsNull(role);
        } else if (status != null) {
            users = userRepository.findByStatusAndDeletedAtIsNull(status);
        } else {
            users = userRepository.findAllByDeletedAtIsNull();
        }
        return users.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateUserStatus(Long id, UserStatusUpdateRequestDto request) {
        log.info("Executing updateUserStatus with id={}", id);
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        user.setStatus(request.status());
        User saved = userRepository.save(user);

        if (request.status() == UserStatus.SUSPENDED || request.status() == UserStatus.INACTIVE) {
            refreshTokenService.deleteByUserId(saved.getId());
            log.info("Revoked tokens for user ID: {} due to status change to {}", saved.getId(), request.status());
        }

        log.info("Updated status for user ID: {} to {}", saved.getId(), request.status());
        return toDto(saved);
    }

    private User findActiveUserByPhone(String phone) {
        return userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found with phone: " + phone));
    }

    private UserProfileResponseDto toDto(User user) {
        return new UserProfileResponseDto(
                user.getId(),
                user.getPhone(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getEmailVerifiedAt(),
                user.getPhoneVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
