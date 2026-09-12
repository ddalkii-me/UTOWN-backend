package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RiderStatus;
import com.utown.utownbackend.entity.TransportType;

public record RiderProfileUpdateRequestDto(
        TransportType transportType,
        Boolean availability,
        RiderStatus status
) {}
