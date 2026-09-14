package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DeliveryAssignment;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    List<DeliveryAssignment> findByRiderId(Long riderId);

    List<DeliveryAssignment> findByOrderId(Long orderId);

    List<DeliveryAssignment> findByStatus(DeliveryAssignmentStatus status);

    List<DeliveryAssignment> findByRiderIdAndStatus(Long riderId, DeliveryAssignmentStatus status);

    List<DeliveryAssignment> findByRiderIdAndStatusIn(Long riderId, Collection<DeliveryAssignmentStatus> statuses);

    List<DeliveryAssignment> findByOrderIdAndStatus(Long orderId, DeliveryAssignmentStatus status);

    List<DeliveryAssignment> findByRiderIdAndOrderId(Long riderId, Long orderId);

    List<DeliveryAssignment> findByRiderIdAndOrderIdAndStatus(Long riderId, Long orderId, DeliveryAssignmentStatus status);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<DeliveryAssignmentStatus> statuses);

    boolean existsByRiderIdAndStatusIn(Long riderId, Collection<DeliveryAssignmentStatus> statuses);

    Optional<DeliveryAssignment> findFirstByOrderIdAndStatusIn(Long orderId, Collection<DeliveryAssignmentStatus> statuses);
}
