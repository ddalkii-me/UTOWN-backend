package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserSecurityTest {

    private UserSecurity userSecurity;
    private CustomUserDetails userDetails;
    private Authentication userAuth;

    @BeforeEach
    void setUp() {
        userSecurity = new UserSecurity();
        User user = TestDataFactory.createUser(10L);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        userDetails = new CustomUserDetails(user);
        userAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("isSelf - returns true when authenticated user ID matches target ID")
    void isSelf_matchingId_returnsTrue() {
        assertThat(userSecurity.isSelf(userAuth, 10L)).isTrue();
    }

    @Test
    @DisplayName("isSelf - returns false when authenticated user ID does not match target ID")
    void isSelf_differentId_returnsFalse() {
        assertThat(userSecurity.isSelf(userAuth, 20L)).isFalse();
    }

    @Test
    @DisplayName("isSelf - returns false when authentication or userId is null")
    void isSelf_nullParams_returnsFalse() {
        assertThat(userSecurity.isSelf(null, 10L)).isFalse();
        assertThat(userSecurity.isSelf(userAuth, null)).isFalse();
    }

    @Test
    @DisplayName("isSelf - returns false when principal is not CustomUserDetails (e.g. String or mock)")
    void isSelf_nonCustomUserDetails_returnsFalse() {
        Authentication stringAuth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
        assertThat(userSecurity.isSelf(stringAuth, 10L)).isFalse();
    }

    @Test
    @DisplayName("isSelfOrAdmin - returns true when caller has ROLE_ADMIN even if IDs differ")
    void isSelfOrAdmin_adminRole_returnsTrue() {
        User adminUser = TestDataFactory.createUser(1L);
        adminUser.setRole(UserRole.ADMIN);
        CustomUserDetails adminDetails = new CustomUserDetails(adminUser);
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        assertThat(userSecurity.isSelfOrAdmin(adminAuth, 99L)).isTrue();
    }

    @Test
    @DisplayName("isSelfOrAdmin - returns true when caller is self (non-admin)")
    void isSelfOrAdmin_self_returnsTrue() {
        assertThat(userSecurity.isSelfOrAdmin(userAuth, 10L)).isTrue();
    }

    @Test
    @DisplayName("isSelfOrAdmin - returns false when caller is neither self nor admin")
    void isSelfOrAdmin_neither_returnsFalse() {
        assertThat(userSecurity.isSelfOrAdmin(userAuth, 99L)).isFalse();
    }
}
