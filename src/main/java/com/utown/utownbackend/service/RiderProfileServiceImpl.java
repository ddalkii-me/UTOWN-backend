package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.RiderProfileRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RiderProfileServiceImpl implements RiderProfileService {

    private final RiderProfileRepository riderProfileRepository;
    private final UserRepository userRepository;

    @Override
    public RiderProfileResponseDto createRiderProfile(RiderProfileRequestDto request) {
        log.info("Creating rider profile for user ID: {}", request.userId());

        User user = userRepository.findById(request.userId())
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.userId()));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot create rider profile for non-active user with status: " + user.getStatus());
        }

        RiderStatus status = request.status() != null ? request.status() : RiderStatus.ACTIVE;
        boolean availability = request.availability() != null ? request.availability() : true;

        if ((status == RiderStatus.SUSPENDED || status == RiderStatus.INACTIVE) && Boolean.TRUE.equals(request.availability())) {
            throw new IllegalArgumentException("Cannot create available rider with status: " + status);
        }
        if (status == RiderStatus.SUSPENDED || status == RiderStatus.INACTIVE) {
            availability = false;
        }

        Optional<RiderProfile> existingProfileOpt = riderProfileRepository.findByUserId(request.userId());
        if (existingProfileOpt.isPresent()) {
            RiderProfile existing = existingProfileOpt.get();
            if (existing.getStatus() != RiderStatus.INACTIVE) {
                throw new ResourceConflictException("Rider profile already exists for user ID: " + request.userId());
            }
            existing.setTransportType(request.transportType());
            existing.setStatus(status);
            existing.setAvailability(availability);
            if (user.getRole() != UserRole.RIDER) {
                user.setRole(UserRole.RIDER);
                userRepository.save(user);
                log.info("Restored role to RIDER for user ID: {}", user.getId());
            }
            RiderProfile saved = riderProfileRepository.save(existing);
            log.info("Reactivated rider profile with ID: {} for user ID: {}", saved.getId(), user.getId());
            return mapToResponseDto(saved);
        }

        if (user.getRole() != UserRole.RIDER) {
            user.setRole(UserRole.RIDER);
            userRepository.save(user);
            log.info("Updated role to RIDER for user ID: {}", user.getId());
        }

        RiderProfile riderProfile = new RiderProfile();
        riderProfile.setUser(user);
        riderProfile.setTransportType(request.transportType());
        riderProfile.setAvailability(availability);
        riderProfile.setStatus(status);

        RiderProfile saved = riderProfileRepository.save(riderProfile);
        log.info("Created rider profile with ID: {} for user ID: {}", saved.getId(), user.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderProfileResponseDto getRiderProfileById(Long id) {
        return riderProfileRepository.findByIdAndUserDeletedAtIsNull(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public RiderProfileResponseDto getRiderProfileByUserId(Long userId) {
        return riderProfileRepository.findByUserIdAndUserDeletedAtIsNull(userId)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiderProfileResponseDto> getAllRiderProfiles(RiderStatus status, Boolean availability) {
        List<RiderProfile> list;

        if (status != null && availability != null) {
            list = riderProfileRepository.findByStatusAndAvailabilityAndUserDeletedAtIsNull(status, availability);
        } else if (status != null) {
            list = riderProfileRepository.findByStatusAndUserDeletedAtIsNull(status);
        } else if (availability != null) {
            list = riderProfileRepository.findByAvailabilityAndUserDeletedAtIsNull(availability);
        } else {
            list = riderProfileRepository.findByUserDeletedAtIsNull();
        }

        return list.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public RiderProfileResponseDto updateRiderProfile(Long id, RiderProfileUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        User user = rider.getUser();
        if (user != null && user.getDeletedAt() != null) {
            throw new EntityNotFoundException("Rider profile not found with id: " + id);
        }

        if (request.transportType() != null) {
            rider.setTransportType(request.transportType());
        }

        RiderProfile saved = riderProfileRepository.save(rider);
        log.info("Updated rider profile with ID: {}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Override
    public RiderProfileResponseDto updateAvailability(Long id, RiderAvailabilityUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        User user = rider.getUser();
        if (user != null && user.getDeletedAt() != null) {
            throw new EntityNotFoundException("Rider profile not found with id: " + id);
        }

        if (Boolean.TRUE.equals(request.availability()) && (rider.getStatus() == RiderStatus.SUSPENDED || rider.getStatus() == RiderStatus.INACTIVE)) {
            throw new IllegalStateException("Cannot set availability to true for rider with status: " + rider.getStatus());
        }

        rider.setAvailability(request.availability());
        RiderProfile saved = riderProfileRepository.save(rider);
        log.info("Updated availability to {} for rider ID: {}", request.availability(), id);
        return mapToResponseDto(saved);
    }

    @Override
    public RiderProfileResponseDto updateStatus(Long id, RiderStatusUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        User user = rider.getUser();
        if (user != null && user.getDeletedAt() != null) {
            throw new EntityNotFoundException("Rider profile not found with id: " + id);
        }

        if (request.status() == RiderStatus.ACTIVE) {
            if (user != null) {
                if (user.getStatus() != UserStatus.ACTIVE) {
                    throw new IllegalStateException("Cannot activate rider profile for non-active user with status: " + user.getStatus());
                }
                if (user.getRole() != UserRole.RIDER) {
                    user.setRole(UserRole.RIDER);
                    userRepository.save(user);
                    log.info("Restored role to RIDER for user ID: {}", user.getId());
                }
            }
        }

        rider.setStatus(request.status());
        if (request.status() == RiderStatus.SUSPENDED || request.status() == RiderStatus.INACTIVE) {
            rider.setAvailability(false);
        }

        RiderProfile saved = riderProfileRepository.save(rider);
        log.info("Updated status to {} for rider ID: {}", request.status(), id);
        return mapToResponseDto(saved);
    }

    @Override
    public void deleteRiderProfile(Long id) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        User user = rider.getUser();
        if (user != null && user.getDeletedAt() != null) {
            throw new EntityNotFoundException("Rider profile not found with id: " + id);
        }

        rider.setStatus(RiderStatus.INACTIVE);
        rider.setAvailability(false);
        if (user != null && user.getRole() == UserRole.RIDER) {
            user.setRole(UserRole.CUSTOMER);
            userRepository.save(user);
        }
        riderProfileRepository.save(rider);
        log.info("Deactivated rider profile with ID: {}", id);
    }

    @Override
    public void deactivateRiderProfileByUserId(Long userId) {
        riderProfileRepository.findByUserId(userId).ifPresent(rider -> {
            rider.setStatus(RiderStatus.INACTIVE);
            rider.setAvailability(false);
            User user = rider.getUser();
            if (user != null && user.getRole() == UserRole.RIDER) {
                user.setRole(UserRole.CUSTOMER);
                userRepository.save(user);
            }
            riderProfileRepository.save(rider);
            log.info("Deactivated rider profile for user ID: {}", userId);
        });
    }

    private RiderProfileResponseDto mapToResponseDto(RiderProfile rider) {
        User user = rider.getUser();
        return new RiderProfileResponseDto(
                rider.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getPhone() : null,
                user != null ? user.getEmail() : null,
                rider.getTransportType(),
                rider.getAvailability(),
                rider.getStatus(),
                rider.getCreatedAt(),
                rider.getUpdatedAt()
        );
    }
}
