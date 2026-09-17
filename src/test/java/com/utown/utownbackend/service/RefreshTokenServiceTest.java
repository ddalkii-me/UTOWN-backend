package com.utown.utownbackend.service;

import com.utown.utownbackend.entity.RefreshToken;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.exception.InvalidTokenException;
import com.utown.utownbackend.repository.RefreshTokenRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 86400000L);

        user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
    }

    @Test
    @DisplayName("createRefreshToken - successfully creates new refresh token when none exists")
    void createRefreshToken_new_success() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(1L);

        assertThat(token).isNotNull();
        assertThat(token.getUser()).isEqualTo(user);
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("createRefreshToken - updates existing refresh token when row already exists")
    void createRefreshToken_existing_updates() {
        RefreshToken existing = new RefreshToken();
        existing.setUser(user);
        existing.setToken("old-token");

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(1L);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotEqualTo("old-token");
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    @DisplayName("createRefreshToken - throws EntityNotFoundException when user not found")
    void createRefreshToken_userNotFound_throws() {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.createRefreshToken(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("verifyExpiration - returns token when valid and not expired")
    void verifyExpiration_validToken_returnsToken() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    @DisplayName("verifyExpiration - deletes token and throws InvalidTokenException when expired")
    void verifyExpiration_expiredToken_throwsInvalidTokenException() {
        RefreshToken token = new RefreshToken();
        token.setExpiryDate(Instant.now().minusSeconds(10));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Refresh token was expired");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    @DisplayName("deleteByUserId - deletes user's refresh token")
    void deleteByUserId_success() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

        refreshTokenService.deleteByUserId(1L);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}
