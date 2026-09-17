package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.DeliveryAssignment;
import com.utown.utownbackend.entity.DeliveryAssignmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    Optional<DeliveryAssignment> findById(Long id);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findAll();

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByRiderId(Long riderId);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByStatus(DeliveryAssignmentStatus status);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByRiderIdAndStatus(Long riderId, DeliveryAssignmentStatus status);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByRiderIdAndStatusIn(Long riderId, Collection<DeliveryAssignmentStatus> statuses);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByOrderIdAndStatus(Long orderId, DeliveryAssignmentStatus status);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByRiderIdAndOrderId(Long riderId, Long orderId);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    List<DeliveryAssignment> findByRiderIdAndOrderIdAndStatus(Long riderId, Long orderId, DeliveryAssignmentStatus status);

    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<DeliveryAssignmentStatus> statuses);

    boolean existsByRiderIdAndStatusIn(Long riderId, Collection<DeliveryAssignmentStatus> statuses);

    @EntityGraph(attributePaths = {"order", "order.restaurant", "rider", "rider.user"})
    Optional<DeliveryAssignment> findFirstByOrderIdAndStatusIn(Long orderId, Collection<DeliveryAssignmentStatus> statuses);
}
