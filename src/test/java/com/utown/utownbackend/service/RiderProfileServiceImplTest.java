package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.RiderProfileRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderProfileServiceImplTest {

    @Mock
    private RiderProfileRepository riderProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RiderProfileServiceImpl riderProfileService;

    private User user;
    private RiderProfile riderProfile;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createUser(1L);
        user.setEmail("rider@example.com");
        user.setName("Rider User");
        user.setPhone("01012345678");
        user.setRole(UserRole.CUSTOMER);

        riderProfile = TestDataFactory.createRiderProfile(
                10L, user, TransportType.MOTORCYCLE, true, RiderStatus.ACTIVE
        );
    }

    @Nested
    @DisplayName("Create RiderProfile Tests")
    class CreateRiderProfileTests {

        @Test
        @DisplayName("createRiderProfile - should create profile and update user role to RIDER")
        void createRiderProfile_shouldSucceed() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.CAR, true, RiderStatus.ACTIVE
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(riderProfileRepository.existsByUserId(1L)).thenReturn(false);
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
                RiderProfile r = invocation.getArgument(0);
                r.setId(10L);
                return r;
            });

            RiderProfileResponseDto response = riderProfileService.createRiderProfile(request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.userName()).isEqualTo("Rider User");
            assertThat(response.userPhone()).isEqualTo("01012345678");
            assertThat(response.transportType()).isEqualTo(TransportType.CAR);
            assertThat(response.availability()).isTrue();
            assertThat(response.status()).isEqualTo(RiderStatus.ACTIVE);
            assertThat(user.getRole()).isEqualTo(UserRole.RIDER);
            verify(userRepository).save(user);
            verify(riderProfileRepository).save(any(RiderProfile.class));
        }

        @Test
        @DisplayName("createRiderProfile - should apply defaults when availability and status are null")
        void createRiderProfile_shouldApplyDefaults() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.WALK, null, null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(riderProfileRepository.existsByUserId(1L)).thenReturn(false);
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(invocation -> {
                RiderProfile r = invocation.getArgument(0);
                r.setId(10L);
                return r;
            });

            RiderProfileResponseDto response = riderProfileService.createRiderProfile(request);

            assertThat(response.availability()).isTrue();
            assertThat(response.status()).isEqualTo(RiderStatus.ACTIVE);
            assertThat(response.transportType()).isEqualTo(TransportType.WALK);
        }

        @Test
        @DisplayName("createRiderProfile - should throw EntityNotFoundException when user does not exist")
        void createRiderProfile_userNotFound_shouldThrow() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    99L, TransportType.BICYCLE, true, RiderStatus.ACTIVE
            );

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderProfileService.createRiderProfile(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("createRiderProfile - should throw EntityNotFoundException when user is soft-deleted")
        void createRiderProfile_userDeleted_shouldThrow() {
            user.setDeletedAt(LocalDateTime.now());
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.BICYCLE, true, RiderStatus.ACTIVE
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> riderProfileService.createRiderProfile(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("createRiderProfile - should throw ResourceConflictException when rider profile already exists")
        void createRiderProfile_alreadyExists_shouldThrow() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.BICYCLE, true, RiderStatus.ACTIVE
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(riderProfileRepository.existsByUserId(1L)).thenReturn(true);

            assertThatThrownBy(() -> riderProfileService.createRiderProfile(request))
                    .isInstanceOf(ResourceConflictException.class)
                    .hasMessageContaining("Rider profile already exists");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("createRiderProfile - should throw IllegalArgumentException when creating SUSPENDED or INACTIVE rider with availability true")
        void createRiderProfile_suspendedWithAvailabilityTrue_shouldThrow() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.BICYCLE, true, RiderStatus.SUSPENDED
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(riderProfileRepository.existsByUserId(1L)).thenReturn(false);

            assertThatThrownBy(() -> riderProfileService.createRiderProfile(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot create available rider with status: SUSPENDED");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("createRiderProfile - should default availability to false when status is SUSPENDED or INACTIVE and availability is null")
        void createRiderProfile_suspendedWithoutAvailability_shouldDefaultFalse() {
            RiderProfileRequestDto request = new RiderProfileRequestDto(
                    1L, TransportType.BICYCLE, null, RiderStatus.SUSPENDED
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(riderProfileRepository.existsByUserId(1L)).thenReturn(false);
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(i -> i.getArgument(0));

            RiderProfileResponseDto response = riderProfileService.createRiderProfile(request);

            assertThat(response.status()).isEqualTo(RiderStatus.SUSPENDED);
            assertThat(response.availability()).isFalse();
        }
    }

    @Nested
    @DisplayName("Get RiderProfile Tests")
    class GetRiderProfileTests {

        @Test
        @DisplayName("getRiderProfileById - should return DTO when found")
        void getRiderProfileById_shouldReturnDto() {
            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            RiderProfileResponseDto response = riderProfileService.getRiderProfileById(10L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
            assertThat(response.userName()).isEqualTo("Rider User");
            assertThat(response.transportType()).isEqualTo(TransportType.MOTORCYCLE);
        }

        @Test
        @DisplayName("getRiderProfileById - should throw EntityNotFoundException when not found")
        void getRiderProfileById_notFound_shouldThrow() {
            when(riderProfileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderProfileService.getRiderProfileById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Rider profile not found");
        }

        @Test
        @DisplayName("getRiderProfileByUserId - should return DTO when found")
        void getRiderProfileByUserId_shouldReturnDto() {
            when(riderProfileRepository.findByUserId(1L)).thenReturn(Optional.of(riderProfile));

            RiderProfileResponseDto response = riderProfileService.getRiderProfileByUserId(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.userId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getRiderProfileByUserId - should throw EntityNotFoundException when not found")
        void getRiderProfileByUserId_notFound_shouldThrow() {
            when(riderProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderProfileService.getRiderProfileByUserId(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Rider profile not found");
        }

        @Test
        @DisplayName("getAllRiderProfiles - with both status and availability filters")
        void getAllRiderProfiles_withBothFilters() {
            when(riderProfileRepository.findByStatusAndAvailability(RiderStatus.ACTIVE, true))
                    .thenReturn(List.of(riderProfile));

            List<RiderProfileResponseDto> list = riderProfileService.getAllRiderProfiles(RiderStatus.ACTIVE, true);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).id()).isEqualTo(10L);
        }

        @Test
        @DisplayName("getAllRiderProfiles - with status filter only")
        void getAllRiderProfiles_withStatusFilter() {
            when(riderProfileRepository.findByStatus(RiderStatus.ACTIVE))
                    .thenReturn(List.of(riderProfile));

            List<RiderProfileResponseDto> list = riderProfileService.getAllRiderProfiles(RiderStatus.ACTIVE, null);

            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("getAllRiderProfiles - with availability filter only")
        void getAllRiderProfiles_withAvailabilityFilter() {
            when(riderProfileRepository.findByAvailability(true))
                    .thenReturn(List.of(riderProfile));

            List<RiderProfileResponseDto> list = riderProfileService.getAllRiderProfiles(null, true);

            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("getAllRiderProfiles - without filters should return all")
        void getAllRiderProfiles_noFilter() {
            when(riderProfileRepository.findAll()).thenReturn(List.of(riderProfile));

            List<RiderProfileResponseDto> list = riderProfileService.getAllRiderProfiles(null, null);

            assertThat(list).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Update RiderProfile Tests")
    class UpdateRiderProfileTests {

        @Test
        @DisplayName("updateRiderProfile - should update transportType")
        void updateRiderProfile_shouldUpdateTransportType() {
            RiderProfileUpdateRequestDto request = new RiderProfileUpdateRequestDto(TransportType.CAR);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(i -> i.getArgument(0));

            RiderProfileResponseDto response = riderProfileService.updateRiderProfile(10L, request);

            assertThat(response.transportType()).isEqualTo(TransportType.CAR);
        }

        @Test
        @DisplayName("updateRiderProfile - should throw EntityNotFoundException when not found")
        void updateRiderProfile_notFound_shouldThrow() {
            RiderProfileUpdateRequestDto request = new RiderProfileUpdateRequestDto(TransportType.CAR);

            when(riderProfileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderProfileService.updateRiderProfile(99L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("updateAvailability - should update availability when active")
        void updateAvailability_shouldSucceed() {
            riderProfile.setStatus(RiderStatus.ACTIVE);
            RiderAvailabilityUpdateRequestDto request = new RiderAvailabilityUpdateRequestDto(false);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(i -> i.getArgument(0));

            RiderProfileResponseDto response = riderProfileService.updateAvailability(10L, request);

            assertThat(response.availability()).isFalse();
        }

        @Test
        @DisplayName("updateAvailability - should throw IllegalStateException when rider is SUSPENDED")
        void updateAvailability_suspended_shouldThrow() {
            riderProfile.setStatus(RiderStatus.SUSPENDED);
            RiderAvailabilityUpdateRequestDto request = new RiderAvailabilityUpdateRequestDto(true);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            assertThatThrownBy(() -> riderProfileService.updateAvailability(10L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot set availability to true for rider with status: SUSPENDED");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateAvailability - should throw IllegalStateException when rider is INACTIVE")
        void updateAvailability_inactive_shouldThrow() {
            riderProfile.setStatus(RiderStatus.INACTIVE);
            RiderAvailabilityUpdateRequestDto request = new RiderAvailabilityUpdateRequestDto(true);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            assertThatThrownBy(() -> riderProfileService.updateAvailability(10L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot set availability to true for rider with status: INACTIVE");

            verify(riderProfileRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateStatus - should update status and force availability to false if SUSPENDED or INACTIVE")
        void updateStatus_suspended_shouldForceAvailabilityFalse() {
            riderProfile.setAvailability(true);
            RiderStatusUpdateRequestDto request = new RiderStatusUpdateRequestDto(RiderStatus.SUSPENDED);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(i -> i.getArgument(0));

            RiderProfileResponseDto response = riderProfileService.updateStatus(10L, request);

            assertThat(response.status()).isEqualTo(RiderStatus.SUSPENDED);
            assertThat(response.availability()).isFalse();
        }

        @Test
        @DisplayName("updateStatus - should keep availability when status remains ACTIVE")
        void updateStatus_active_shouldPreserveAvailability() {
            riderProfile.setAvailability(true);
            RiderStatusUpdateRequestDto request = new RiderStatusUpdateRequestDto(RiderStatus.ACTIVE);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));
            when(riderProfileRepository.save(any(RiderProfile.class))).thenAnswer(i -> i.getArgument(0));

            RiderProfileResponseDto response = riderProfileService.updateStatus(10L, request);

            assertThat(response.status()).isEqualTo(RiderStatus.ACTIVE);
            assertThat(response.availability()).isTrue();
        }
    }

    @Nested
    @DisplayName("Delete RiderProfile Tests")
    class DeleteRiderProfileTests {

        @Test
        @DisplayName("deleteRiderProfile - should soft-deactivate profile and revert user role to CUSTOMER")
        void deleteRiderProfile_shouldSoftDeactivate() {
            user.setRole(UserRole.RIDER);
            riderProfile.setUser(user);
            riderProfile.setStatus(RiderStatus.ACTIVE);
            riderProfile.setAvailability(true);

            when(riderProfileRepository.findById(10L)).thenReturn(Optional.of(riderProfile));

            riderProfileService.deleteRiderProfile(10L);

            assertThat(riderProfile.getStatus()).isEqualTo(RiderStatus.INACTIVE);
            assertThat(riderProfile.getAvailability()).isFalse();
            assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
            verify(riderProfileRepository).save(riderProfile);
            verify(userRepository).save(user);
            verify(riderProfileRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteRiderProfile - should throw EntityNotFoundException when profile does not exist")
        void deleteRiderProfile_notFound_shouldThrow() {
            when(riderProfileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> riderProfileService.deleteRiderProfile(99L))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(riderProfileRepository, never()).delete(any());
            verify(riderProfileRepository, never()).save(any());
        }
    }
}
