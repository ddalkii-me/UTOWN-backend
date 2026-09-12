package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.RiderStatus;

import java.util.List;

public interface RiderProfileService {

    RiderProfileResponseDto createRiderProfile(RiderProfileRequestDto request);

    RiderProfileResponseDto getRiderProfileById(Long id);

    RiderProfileResponseDto getRiderProfileByUserId(Long userId);

    List<RiderProfileResponseDto> getAllRiderProfiles(RiderStatus status, Boolean availability);

    RiderProfileResponseDto updateRiderProfile(Long id, RiderProfileUpdateRequestDto request);

    RiderProfileResponseDto updateAvailability(Long id, RiderAvailabilityUpdateRequestDto request);

    RiderProfileResponseDto updateStatus(Long id, RiderStatusUpdateRequestDto request);

    void deleteRiderProfile(Long id);
}
