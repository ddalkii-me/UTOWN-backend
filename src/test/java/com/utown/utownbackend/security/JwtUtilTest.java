package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private CustomUserDetails userDetails;

    // 256-bit test secret
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 1000 * 60 * 60; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, EXPIRATION_MS);

        User user = new User();
        user.setId(1L);
        user.setPhone("01012345678");
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("secret");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("generateToken and extractUsername - successfully round trips username (phone)")
    void generateToken_extractUsername_success() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        String extractedUsername = jwtUtil.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("validateToken - returns true for valid token and matching user details")
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(userDetails);

        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("validateToken - returns false for different user details")
    void validateToken_differentUser_returnsFalse() {
        String token = jwtUtil.generateToken(userDetails);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setPhone("01099998888");
        otherUser.setName("Other User");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("secret");
        otherUser.setRole(UserRole.CUSTOMER);
        otherUser.setStatus(UserStatus.ACTIVE);

        CustomUserDetails otherDetails = new CustomUserDetails(otherUser);

        boolean isValid = jwtUtil.validateToken(token, otherDetails);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("validateToken - returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        // JwtUtil with negative expiration to simulate immediate expiry
        JwtUtil expiredJwtUtil = new JwtUtil(TEST_SECRET, -1000);
        String expiredToken = expiredJwtUtil.generateToken(userDetails);

        boolean isValid = expiredJwtUtil.validateToken(expiredToken, userDetails);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("generateToken with extraClaims - successfully embeds and reads custom claims")
    void generateToken_withExtraClaims() {
        String token = jwtUtil.generateToken(Map.of("role", "CUSTOMER"), userDetails.getUsername());

        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
        assertThat(role).isEqualTo("CUSTOMER");
    }
}
