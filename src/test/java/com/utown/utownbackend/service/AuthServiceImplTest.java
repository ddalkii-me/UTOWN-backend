package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AuthResponseDto;
import com.utown.utownbackend.dto.LoginRequestDto;
import com.utown.utownbackend.dto.RegisterRequestDto;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.UserRepository;
import com.utown.utownbackend.security.CustomUserDetails;
import com.utown.utownbackend.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("register - successfully registers user with CUSTOMER role and returns token")
    void register_success() {
        RegisterRequestDto request = new RegisterRequestDto(
                "010-1234-5678",
                "secretPassword",
                "secretPassword",
                "Test Customer",
                "test@example.com",
                "123456"
        );

        when(userRepository.existsByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secretPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("jwt-mock-token");
        com.utown.utownbackend.entity.RefreshToken rt = new com.utown.utownbackend.entity.RefreshToken();
        rt.setToken("mock-refresh-token");
        when(refreshTokenService.createRefreshToken(10L)).thenReturn(rt);

        AuthResponseDto response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-mock-token");
        assertThat(response.refreshToken()).isEqualTo("mock-refresh-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPhone()).isEqualTo("+821012345678");
        assertThat(savedUser.getName()).isEqualTo("Test Customer");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("hashedPassword");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("register - throws ResourceConflictException when phone already exists")
    void register_duplicatePhone_throwsConflict() {
        RegisterRequestDto request = new RegisterRequestDto(
                "010-1234-5678",
                "secretPassword",
                "secretPassword",
                "Test Customer",
                "test@example.com",
                "123456"
        );

        when(userRepository.existsByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Phone number is already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - throws ResourceConflictException when email already exists")
    void register_duplicateEmail_throwsConflict() {
        RegisterRequestDto request = new RegisterRequestDto(
                "010-1234-5678",
                "secretPassword",
                "secretPassword",
                "Test Customer",
                "test@example.com",
                "123456"
        );

        when(userRepository.existsByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - successfully authenticates and returns token")
    void login_success() {
        LoginRequestDto request = new LoginRequestDto("010-1234-5678", "secretPassword");

        User user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtUtil.generateToken(userDetails)).thenReturn("login-jwt-token");
        
        when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(java.util.Optional.of(user));
        com.utown.utownbackend.entity.RefreshToken rt = new com.utown.utownbackend.entity.RefreshToken();
        rt.setToken("mock-refresh-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(rt);

        AuthResponseDto response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("login-jwt-token");
        assertThat(response.refreshToken()).isEqualTo("mock-refresh-token");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login - propagates BadCredentialsException on invalid credentials")
    void login_badCredentials_throwsException() {
        LoginRequestDto request = new LoginRequestDto("010-1234-5678", "wrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }

    @Test
    @DisplayName("refreshToken - returns new tokens when valid refresh token is provided")
    void refreshToken_success() {
        com.utown.utownbackend.dto.TokenRefreshRequestDto request = new com.utown.utownbackend.dto.TokenRefreshRequestDto("valid-refresh-token");
        com.utown.utownbackend.entity.RefreshToken rt = new com.utown.utownbackend.entity.RefreshToken();
        User user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        user.setStatus(UserStatus.ACTIVE);
        rt.setUser(user);
        
        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(java.util.Optional.of(rt));
        when(refreshTokenService.verifyExpiration(rt)).thenReturn(rt);
        when(jwtUtil.generateToken(any(com.utown.utownbackend.security.CustomUserDetails.class))).thenReturn("new-jwt-token");
        
        com.utown.utownbackend.entity.RefreshToken newRt = new com.utown.utownbackend.entity.RefreshToken();
        newRt.setToken("valid-refresh-token");
        when(refreshTokenService.createRefreshToken(1L)).thenReturn(newRt);
        
        AuthResponseDto response = authService.refreshToken(request);
        
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("new-jwt-token");
        assertThat(response.refreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    @DisplayName("refreshToken - throws exception when refresh token is invalid")
    void refreshToken_invalidToken_throwsException() {
        com.utown.utownbackend.dto.TokenRefreshRequestDto request = new com.utown.utownbackend.dto.TokenRefreshRequestDto("invalid-refresh-token");
        
        when(refreshTokenService.findByToken("invalid-refresh-token")).thenReturn(java.util.Optional.empty());
        
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(com.utown.utownbackend.exception.InvalidTokenException.class)
                .hasMessageContaining("Refresh token is invalid or missing!");
    }

    @Test
    @DisplayName("login - throws DisabledException when user is suspended")
    void login_suspendedUser_throwsDisabledException() {
        LoginRequestDto request = new LoginRequestDto("010-1234-5678", "secretPassword");

        User user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        user.setPassword("hashedPassword");
        user.setStatus(UserStatus.SUSPENDED);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        
        when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessageContaining("User is not active");
    }

    @Test
    @DisplayName("refreshToken - throws DisabledException when user is suspended")
    void refreshToken_suspendedUser_throwsDisabledException() {
        com.utown.utownbackend.dto.TokenRefreshRequestDto request = new com.utown.utownbackend.dto.TokenRefreshRequestDto("valid-refresh-token");
        com.utown.utownbackend.entity.RefreshToken rt = new com.utown.utownbackend.entity.RefreshToken();
        User user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        user.setStatus(UserStatus.SUSPENDED);
        rt.setUser(user);
        
        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(java.util.Optional.of(rt));
        when(refreshTokenService.verifyExpiration(rt)).thenReturn(rt);
        
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessageContaining("User is not active");
    }

    @Test
    @DisplayName("logout - deletes refresh token by username")
    void logout_success() {
        User user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        
        when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678")).thenReturn(java.util.Optional.of(user));
        
        authService.logout("+821012345678");
        
        verify(refreshTokenService).deleteByUserId(1L);
    }
}
