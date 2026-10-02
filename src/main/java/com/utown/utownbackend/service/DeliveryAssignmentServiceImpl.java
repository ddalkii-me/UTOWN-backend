package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DeliveryAssignmentRequestDto;
import com.utown.utownbackend.dto.DeliveryAssignmentResponseDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.DeliveryAssignmentRepository;
import com.utown.utownbackend.repository.OrderRepository;
import com.utown.utownbackend.repository.RiderProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private static final Set<DeliveryAssignmentStatus> ACTIVE_STATUSES = Set.of(
            DeliveryAssignmentStatus.ASSIGNED,
            DeliveryAssignmentStatus.ACCEPTED,
            DeliveryAssignmentStatus.PICKED_UP
    );

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final OrderRepository orderRepository;
    private final RiderProfileRepository riderProfileRepository;

    @Override
    public DeliveryAssignmentResponseDto createAssignment(DeliveryAssignmentRequestDto request) {
        log.info("Executing createAssignment");
        log.info("Creating delivery assignment for order ID: {} and rider ID: {}", request.orderId(), request.riderId());

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + request.orderId()));

        if (order.getStatus() != OrderStatus.ACCEPTED
                && order.getStatus() != OrderStatus.IN_PREPARATION
                && order.getStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new IllegalStateException("Order is not ready for delivery assignment (status: " + order.getStatus() + ")");
        }

        if (deliveryAssignmentRepository.existsByOrderIdAndStatusIn(request.orderId(), ACTIVE_STATUSES)) {
            throw new ResourceConflictException("Order already has an active delivery assignment: " + request.orderId());
        }

        RiderProfile rider = riderProfileRepository.findById(request.riderId())
                .orElseThrow(() -> new EntityNotFoundException("Rider profile not found with id: " + request.riderId()));

        if (rider.getStatus() != RiderStatus.ACTIVE) {
            throw new IllegalStateException("Rider is not active (status: " + rider.getStatus() + ")");
        }

        if (!Boolean.TRUE.equals(rider.getAvailability())) {
            throw new IllegalStateException("Rider is not available for delivery");
        }

        if (deliveryAssignmentRepository.existsByRiderIdAndStatusIn(request.riderId(), ACTIVE_STATUSES)) {
            throw new ResourceConflictException("Rider already has an active delivery assignment: " + request.riderId());
        }

        rider.setAvailability(false);
        riderProfileRepository.save(rider);

        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.setOrder(order);
        assignment.setRider(rider);
        assignment.setStatus(DeliveryAssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Created delivery assignment with ID: {} for order ID: {}", saved.getId(), order.getId());

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAssignmentResponseDto getAssignmentById(Long id) {
        log.info("Executing getAssignmentById with id={}", id);
        return deliveryAssignmentRepository.findById(id)
                .map(this::mapToResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponseDto> getAssignments(Long riderId, Long orderId, DeliveryAssignmentStatus status) {
        log.info("Executing getAssignments with riderId={}, orderId={}, status={}", riderId, orderId, status);
        List<DeliveryAssignment> list;

        if (riderId != null && orderId != null && status != null) {
            list = deliveryAssignmentRepository.findByRiderIdAndOrderIdAndStatus(riderId, orderId, status);
        } else if (riderId != null && orderId != null) {
            list = deliveryAssignmentRepository.findByRiderIdAndOrderId(riderId, orderId);
        } else if (riderId != null && status != null) {
            list = deliveryAssignmentRepository.findByRiderIdAndStatus(riderId, status);
        } else if (orderId != null && status != null) {
            list = deliveryAssignmentRepository.findByOrderIdAndStatus(orderId, status);
        } else if (riderId != null) {
            list = deliveryAssignmentRepository.findByRiderId(riderId);
        } else if (orderId != null) {
            list = deliveryAssignmentRepository.findByOrderId(orderId);
        } else if (status != null) {
            list = deliveryAssignmentRepository.findByStatus(status);
        } else {
            list = deliveryAssignmentRepository.findAll();
        }

        return list.stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public DeliveryAssignmentResponseDto acceptAssignment(Long id) {
        log.info("Executing acceptAssignment with id={}", id);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found with id: " + id));

        if (assignment.getStatus() != DeliveryAssignmentStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot accept assignment in status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Rider accepted delivery assignment ID: {}", id);
        return mapToResponseDto(saved);
    }

    @Override
    public DeliveryAssignmentResponseDto pickupDelivery(Long id) {
        log.info("Executing pickupDelivery with id={}", id);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found with id: " + id));

        if (assignment.getStatus() != DeliveryAssignmentStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot pickup delivery in status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.PICKED_UP);
        assignment.setPickedUpAt(LocalDateTime.now());

        Order order = assignment.getOrder();
        if (order != null) {
            order.setPickedUpAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Rider picked up order for delivery assignment ID: {}", id);
        return mapToResponseDto(saved);
    }

    @Override
    public DeliveryAssignmentResponseDto completeDelivery(Long id) {
        log.info("Executing completeDelivery with id={}", id);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found with id: " + id));

        if (assignment.getStatus() != DeliveryAssignmentStatus.PICKED_UP) {
            throw new IllegalStateException("Cannot complete delivery in status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.DELIVERED);
        assignment.setDeliveredAt(LocalDateTime.now());

        Order order = assignment.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            orderRepository.save(order);
        }

        RiderProfile rider = assignment.getRider();
        if (rider != null && rider.getStatus() == RiderStatus.ACTIVE) {
            rider.setAvailability(true);
            riderProfileRepository.save(rider);
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Delivery completed for assignment ID: {}", id);
        return mapToResponseDto(saved);
    }

    @Override
    public DeliveryAssignmentResponseDto cancelAssignment(Long id) {
        log.info("Executing cancelAssignment with id={}", id);
        DeliveryAssignment assignment = deliveryAssignmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Delivery assignment not found with id: " + id));

        if (assignment.getStatus() == DeliveryAssignmentStatus.DELIVERED
                || assignment.getStatus() == DeliveryAssignmentStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel delivery assignment in status: " + assignment.getStatus());
        }

        assignment.setStatus(DeliveryAssignmentStatus.CANCELLED);

        RiderProfile rider = assignment.getRider();
        if (rider != null && rider.getStatus() == RiderStatus.ACTIVE) {
            rider.setAvailability(true);
            riderProfileRepository.save(rider);
        }

        DeliveryAssignment saved = deliveryAssignmentRepository.save(assignment);
        log.info("Cancelled delivery assignment ID: {}", id);
        return mapToResponseDto(saved);
    }

    private DeliveryAssignmentResponseDto mapToResponseDto(DeliveryAssignment assignment) {
        Order order = assignment.getOrder();
        RiderProfile rider = assignment.getRider();
        User riderUser = rider != null ? rider.getUser() : null;
        Restaurant restaurant = order != null ? order.getRestaurant() : null;

        return new DeliveryAssignmentResponseDto(
                assignment.getId(),
                order != null ? order.getId() : null,
                order != null ? order.getOrderNumber() : null,
                restaurant != null ? restaurant.getId() : null,
                restaurant != null ? restaurant.getName() : null,
                rider != null ? rider.getId() : null,
                riderUser != null ? riderUser.getName() : null,
                riderUser != null ? riderUser.getPhone() : null,
                rider != null ? rider.getTransportType() : null,
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getAcceptedAt(),
                assignment.getPickedUpAt(),
                assignment.getDeliveredAt(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }
}
