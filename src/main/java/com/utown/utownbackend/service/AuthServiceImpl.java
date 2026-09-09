package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.dto.TokenRefreshRequestDto;
import com.utown.utownbackend.entity.RefreshToken;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.security.CustomUserDetails;
import com.utown.utownbackend.security.JwtUtil;
import com.utown.utownbackend.util.PhoneUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        String normalizedPhone = PhoneUtil.normalizePhone(request.phone());
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        if (userRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone)) {
            throw new ResourceConflictException("Phone number is already in use");
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw new ResourceConflictException("Email is already in use");
        }

        User user = new User();
        user.setPhone(normalizedPhone);
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        log.info("Registered new user with ID: {} and phone: {}", savedUser.getId(), normalizedPhone);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser.getId());

        return new AuthResponseDto(token, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedPhone = PhoneUtil.normalizePhone(request.phone());
        if (normalizedPhone == null) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedPhone, request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        
        User user = userRepository.findByPhoneAndDeletedAtIsNull(normalizedPhone)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        log.info("User with phone: {} successfully authenticated", normalizedPhone);
        return new AuthResponseDto(token, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponseDto refreshToken(TokenRefreshRequestDto request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    CustomUserDetails userDetails = new CustomUserDetails(user);
                    String token = jwtUtil.generateToken(userDetails);
                    return new AuthResponseDto(token, request.refreshToken());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public void logout(String phone) {
        User user = userRepository.findByPhoneAndDeletedAtIsNull(phone)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        refreshTokenService.deleteByUserId(user.getId());
        log.info("User with phone: {} logged out", phone);
    }
}
