package com.utown.utownbackend.security;

import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import com.utown.utownbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setPhone("+821012345678");
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("loadUserByUsername - normalizes phone and returns UserDetails")
    void loadUserByUsername_success() {
        when(userRepository.findByPhoneAndDeletedAtIsNull("+821012345678"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("010-1234-5678");

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("+821012345678");
        assertThat(userDetails.getPassword()).isEqualTo("hashedPassword");
        assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");

        verify(userRepository).findByPhoneAndDeletedAtIsNull("+821012345678");
    }

    @Test
    @DisplayName("loadUserByUsername - throws UsernameNotFoundException when user is not found")
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByPhoneAndDeletedAtIsNull("+821099998888"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("010-9999-8888"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with phone: 010-9999-8888");
    }

    @Test
    @DisplayName("loadUserByUsername - throws UsernameNotFoundException when phone is blank")
    void loadUserByUsername_blankPhone_throwsException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Invalid phone number");
    }
}
