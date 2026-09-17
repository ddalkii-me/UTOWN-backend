package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.repository.RiderProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RiderProfileSecurityTest {

    private final RiderProfileRepository riderProfileRepository =
            mock(RiderProfileRepository.class);

    private final RiderProfileSecurity riderProfileSecurity =
            new RiderProfileSecurity(riderProfileRepository);

    @Test
    void isOwner_riderOwnProfile_returnsTrue() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);
        User user = mock(User.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        when(riderProfileRepository.findByIdAndUserDeletedAtIsNull(10L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertTrue(result);
    }

    @Test
    void isOwner_riderOtherUsersProfile_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);
        User user = mock(User.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        when(riderProfileRepository.findByIdAndUserDeletedAtIsNull(10L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(2L);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_adminWithMatchingUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);
        User user = mock(User.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.ADMIN);

        when(riderProfileRepository.findByIdAndUserDeletedAtIsNull(10L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_customerWithMatchingUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);
        User user = mock(User.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.CUSTOMER);

        when(riderProfileRepository.findByIdAndUserDeletedAtIsNull(10L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_unauthenticated_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_wrongPrincipal_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwner_nullRiderProfileId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result = riderProfileSecurity.isOwner(null, authentication);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwner_nullAuthenticatedUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(null);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result = riderProfileSecurity.isOwner(10L, authentication);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_riderOwnUserId_returnsTrue() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result = riderProfileSecurity.isOwnerByUserId(1L, authentication);

        assertTrue(result);
    }

    @Test
    void isOwnerByUserId_riderOtherUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result = riderProfileSecurity.isOwnerByUserId(2L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwnerByUserId_adminWithMatchingUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.ADMIN);

        boolean result = riderProfileSecurity.isOwnerByUserId(1L, authentication);

        assertFalse(result);
    }

    @Test
    void isOwnerByUserId_nullUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result = riderProfileSecurity.isOwnerByUserId(null, authentication);

        assertFalse(result);
    }
}