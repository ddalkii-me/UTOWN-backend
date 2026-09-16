package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartSecurityTest {

    private final CartSecurity cartSecurity = new CartSecurity();

    @Test
    void isOwner_customerOwnCart_returnsTrue() {
        Authentication authentication = mock(Authentication.class);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.CUSTOMER);

        boolean result = cartSecurity.isOwner(1L, authentication);

        assertTrue(result);
    }

    @Test
    void isOwner_customerOtherUsersCart_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.CUSTOMER);

        boolean result = cartSecurity.isOwner(2L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_admin_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.ADMIN);

        boolean result = cartSecurity.isOwner(2L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_unauthenticated_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = cartSecurity.isOwner(1L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_wrongPrincipal_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        boolean result = cartSecurity.isOwner(1L, authentication);

        assertFalse(result);
    }
}