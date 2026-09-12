package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.RiderProfileRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        if (riderProfileRepository.existsByUserId(request.userId())) {
            throw new ResourceConflictException("Rider profile already exists for user ID: " + request.userId());
        }

        if (user.getRole() != UserRole.RIDER) {
            user.setRole(UserRole.RIDER);
            userRepository.save(user);
            log.info("Updated role to RIDER for user ID: {}", user.getId());
        }

        RiderProfile riderProfile = new RiderProfile();
        riderProfile.setUser(user);
        riderProfile.setTransportType(request.transportType());
        riderProfile.setAvailability(request.availability() != null ? request.availability() : true);
        riderProfile.setStatus(request.status() != null ? request.status() : RiderStatus.ACTIVE);

        RiderProfile saved = riderProfileRepository.save(riderProfile);
        log.info("Created rider profile with ID: {} for user ID: {}", saved.getId(), user.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RiderProfileResponseDto getRiderProfileById(Long id) {
        return riderProfileRepository.findById(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public RiderProfileResponseDto getRiderProfileByUserId(Long userId) {
        return riderProfileRepository.findByUserId(userId)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found for user id: " + userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiderProfileResponseDto> getAllRiderProfiles(RiderStatus status, Boolean availability) {
        List<RiderProfile> list;

        if (status != null && availability != null) {
            list = riderProfileRepository.findByStatusAndAvailability(status, availability);
        } else if (status != null) {
            list = riderProfileRepository.findByStatus(status);
        } else if (availability != null) {
            list = riderProfileRepository.findByAvailability(availability);
        } else {
            list = riderProfileRepository.findAll();
        }

        return list.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public RiderProfileResponseDto updateRiderProfile(Long id, RiderProfileUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        if (request.transportType() != null) {
            rider.setTransportType(request.transportType());
        }

        if (request.availability() != null) {
            rider.setAvailability(request.availability());
        }

        if (request.status() != null) {
            rider.setStatus(request.status());
            if (request.status() == RiderStatus.SUSPENDED || request.status() == RiderStatus.INACTIVE) {
                rider.setAvailability(false);
            }
        }

        RiderProfile saved = riderProfileRepository.save(rider);
        log.info("Updated rider profile with ID: {}", saved.getId());
        return mapToResponseDto(saved);
    }

    @Override
    public RiderProfileResponseDto updateAvailability(Long id, RiderAvailabilityUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

        rider.setAvailability(request.availability());
        RiderProfile saved = riderProfileRepository.save(rider);
        log.info("Updated availability to {} for rider ID: {}", request.availability(), id);
        return mapToResponseDto(saved);
    }

    @Override
    public RiderProfileResponseDto updateStatus(Long id, RiderStatusUpdateRequestDto request) {
        RiderProfile rider = riderProfileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + id));

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

        riderProfileRepository.delete(rider);
        log.info("Deleted rider profile with ID: {}", id);
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
