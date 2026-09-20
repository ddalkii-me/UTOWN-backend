package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.RiderStatus;
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
        when(riderProfile.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

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
        when(riderProfile.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(user.getId()).thenReturn(2L);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

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
        when(riderProfile.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

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
        when(riderProfile.getStatus()).thenReturn(RiderStatus.ACTIVE);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

        assertFalse(result);
    }

    @Test
    void isOwner_inactiveRider_returnsFalse() {
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
        when(riderProfile.getStatus()).thenReturn(RiderStatus.INACTIVE);
        when(user.getId()).thenReturn(1L);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

        assertFalse(result);
    }

    @Test
    void isOwner_unauthenticated_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

        assertFalse(result);
    }


    @Test
    void isOwner_wrongPrincipal_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

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

        boolean result = riderProfileSecurity.isOwner(authentication, null);

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

        boolean result = riderProfileSecurity.isOwner(authentication, 10L);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_riderOwnActiveProfile_returnsTrue() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        when(riderProfileRepository.findByUserIdAndUserDeletedAtIsNull(1L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getStatus()).thenReturn(RiderStatus.ACTIVE);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertTrue(result);

        verify(riderProfileRepository)
                .findByUserIdAndUserDeletedAtIsNull(1L);
    }

    @Test
    void isOwnerByUserId_riderOwnSuspendedProfile_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        RiderProfile riderProfile = mock(RiderProfile.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        when(riderProfileRepository.findByUserIdAndUserDeletedAtIsNull(1L))
                .thenReturn(Optional.of(riderProfile));
        when(riderProfile.getStatus()).thenReturn(RiderStatus.SUSPENDED);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertFalse(result);

        verify(riderProfileRepository)
                .findByUserIdAndUserDeletedAtIsNull(1L);
    }

    @Test
    void isOwnerByUserId_riderOtherUser_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 2L);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_customer_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.CUSTOMER);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_unauthenticated_returnsFalse() {
        Authentication authentication = mock(Authentication.class);

        when(authentication.isAuthenticated()).thenReturn(false);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_nullUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, null);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_nullAuthenticatedUserId_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(null);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertFalse(result);

        verifyNoInteractions(riderProfileRepository);
    }

    @Test
    void isOwnerByUserId_profileNotFound_returnsFalse() {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getRole()).thenReturn(UserRole.RIDER);

        when(riderProfileRepository.findByUserIdAndUserDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        boolean result =
                riderProfileSecurity.isOwnerByUserId(authentication, 1L);

        assertFalse(result);
    }

}