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
import com.utown.utownbackend.util.TestDataFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private final String phone = "01012345678";

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(1L);
        user.setPhone(phone);
        user.setEmail("user1@example.com");
        user.setPassword("encodedOldPassword");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
    }

    @Nested
    @DisplayName("getCurrentUserProfile")
    class GetCurrentUserProfileTests {

        @Test
        @DisplayName("success - returns profile for authenticated user")
        void getCurrentUserProfile_success() {
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));

            UserProfileResponseDto response = userService.getCurrentUserProfile(phone);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.phone()).isEqualTo(phone);
            assertThat(response.email()).isEqualTo("user1@example.com");
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void getCurrentUserProfile_notFound() {
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUserProfile(phone))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("updateCurrentUserProfile")
    class UpdateCurrentUserProfileTests {

        @Test
        @DisplayName("success - updates name and email")
        void updateCurrentUserProfile_success() {
            UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("New Name", "newemail@example.com");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(userRepository.findByEmailAndDeletedAtIsNull("newemail@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto response = userService.updateCurrentUserProfile(phone, request);

            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.email()).isEqualTo("newemail@example.com");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("same email - allowed without conflict")
        void updateCurrentUserProfile_sameEmail_allowed() {
            UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("New Name", "user1@example.com");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto response = userService.updateCurrentUserProfile(phone, request);

            assertThat(response.name()).isEqualTo("New Name");
            verify(userRepository, never()).findByEmailAndDeletedAtIsNull(any());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("email taken by another user - throws ResourceConflictException")
        void updateCurrentUserProfile_emailTaken_throwsConflict() {
            UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("New Name", "other@example.com");
            User otherUser = TestDataFactory.createUser(2L);
            otherUser.setEmail("other@example.com");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(userRepository.findByEmailAndDeletedAtIsNull("other@example.com")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.updateCurrentUserProfile(phone, request))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Email is already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void updateCurrentUserProfile_notFound() {
            UserProfileUpdateRequestDto request = new UserProfileUpdateRequestDto("New Name", "email@example.com");
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateCurrentUserProfile(phone, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("success - encodes new password, updates user, invalidates refresh tokens")
        void changePassword_success() {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldSecret123", "newSecret123", "newSecret123");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldSecret123", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.matches("newSecret123", "encodedOldPassword")).thenReturn(false);
            when(passwordEncoder.encode("newSecret123")).thenReturn("newEncodedPassword");

            userService.changePassword(phone, request);

            assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
            verify(userRepository).save(user);
            verify(refreshTokenService).deleteByUserId(user.getId());
        }

        @Test
        @DisplayName("wrong current password - throws IllegalArgumentException")
        void changePassword_wrongOldPassword_throwsException() {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto("wrongSecret", "newSecret123", "newSecret123");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongSecret", "encodedOldPassword")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(phone, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
            verify(refreshTokenService, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("new password same as current password - throws IllegalArgumentException")
        void changePassword_samePassword_throwsException() {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldSecret123", "oldSecret123", "oldSecret123");

            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldSecret123", "encodedOldPassword")).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(phone, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New password must be different from current password");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void changePassword_notFound() {
            ChangePasswordRequestDto request = new ChangePasswordRequestDto("oldSecret123", "newSecret123", "newSecret123");
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(phone, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCurrentUser")
    class DeleteCurrentUserTests {

        @Test
        @DisplayName("success - sets deletedAt, sets status to INACTIVE, deletes refresh tokens")
        void deleteCurrentUser_success() {
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.of(user));

            userService.deleteCurrentUser(phone);

            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
            verify(userRepository).save(user);
            verify(refreshTokenService).deleteByUserId(user.getId());
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void deleteCurrentUser_notFound() {
            when(userRepository.findByPhoneAndDeletedAtIsNull(phone)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteCurrentUser(phone))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserByIdTests {

        @Test
        @DisplayName("success - returns profile for user by ID")
        void getUserById_success() {
            when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

            UserProfileResponseDto response = userService.getUserById(1L);

            assertThat(response.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void getUserById_notFound() {
            when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUsers")
    class GetUsersTests {

        @Test
        @DisplayName("no filters - returns all active users")
        void getUsers_noFilters() {
            when(userRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(user));

            List<UserProfileResponseDto> result = userService.getUsers(null, null);

            assertThat(result).hasSize(1);
            verify(userRepository).findAllByDeletedAtIsNull();
        }

        @Test
        @DisplayName("filter by role only")
        void getUsers_filterByRole() {
            when(userRepository.findByRoleAndDeletedAtIsNull(UserRole.CUSTOMER)).thenReturn(List.of(user));

            List<UserProfileResponseDto> result = userService.getUsers(UserRole.CUSTOMER, null);

            assertThat(result).hasSize(1);
            verify(userRepository).findByRoleAndDeletedAtIsNull(UserRole.CUSTOMER);
        }

        @Test
        @DisplayName("filter by status only")
        void getUsers_filterByStatus() {
            when(userRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)).thenReturn(List.of(user));

            List<UserProfileResponseDto> result = userService.getUsers(null, UserStatus.ACTIVE);

            assertThat(result).hasSize(1);
            verify(userRepository).findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("filter by role and status")
        void getUsers_filterByRoleAndStatus() {
            when(userRepository.findByRoleAndStatusAndDeletedAtIsNull(UserRole.CUSTOMER, UserStatus.ACTIVE)).thenReturn(List.of(user));

            List<UserProfileResponseDto> result = userService.getUsers(UserRole.CUSTOMER, UserStatus.ACTIVE);

            assertThat(result).hasSize(1);
            verify(userRepository).findByRoleAndStatusAndDeletedAtIsNull(UserRole.CUSTOMER, UserStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("updateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("success - updates status to SUSPENDED and revokes refresh tokens")
        void updateUserStatus_toSuspended_revokesTokens() {
            UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(UserStatus.SUSPENDED);
            when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto response = userService.updateUserStatus(1L, request);

            assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            verify(userRepository).save(user);
            verify(refreshTokenService).deleteByUserId(1L);
        }

        @Test
        @DisplayName("success - updates status to ACTIVE does not revoke tokens")
        void updateUserStatus_toActive_doesNotRevokeTokens() {
            user.setStatus(UserStatus.INACTIVE);
            UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(UserStatus.ACTIVE);
            when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserProfileResponseDto response = userService.updateUserStatus(1L, request);

            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
            verify(userRepository).save(user);
            verify(refreshTokenService, never()).deleteByUserId(any());
        }

        @Test
        @DisplayName("user not found - throws EntityNotFoundException")
        void updateUserStatus_notFound() {
            UserStatusUpdateRequestDto request = new UserStatusUpdateRequestDto(UserStatus.ACTIVE);
            when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserStatus(99L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
