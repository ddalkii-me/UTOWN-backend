package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;

import java.util.List;

public interface DeliveryAssignmentService {

    DeliveryAssignmentResponseDto createAssignment(DeliveryAssignmentRequestDto request);

    DeliveryAssignmentResponseDto getAssignmentById(Long id);

    List<DeliveryAssignmentResponseDto> getAssignments(Long riderId, Long orderId, DeliveryAssignmentStatus status);

    DeliveryAssignmentResponseDto acceptAssignment(Long id);

    DeliveryAssignmentResponseDto pickupDelivery(Long id);

    DeliveryAssignmentResponseDto completeDelivery(Long id);

    DeliveryAssignmentResponseDto cancelAssignment(Long id);
}
