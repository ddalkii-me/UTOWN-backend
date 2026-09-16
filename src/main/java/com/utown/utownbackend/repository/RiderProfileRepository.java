package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.RiderProfile;
import com.utown.utownbackend.entity.RiderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiderProfileRepository extends JpaRepository<RiderProfile, Long> {

    Optional<RiderProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<RiderProfile> findByStatus(RiderStatus status);

    List<RiderProfile> findByAvailability(Boolean availability);

    List<RiderProfile> findByStatusAndAvailability(RiderStatus status, Boolean availability);

    @EntityGraph(attributePaths = {"user"})
    Optional<RiderProfile> findByIdAndUserDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = {"user"})
    Optional<RiderProfile> findByUserIdAndUserDeletedAtIsNull(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<RiderProfile> findByUserDeletedAtIsNull();

    @EntityGraph(attributePaths = {"user"})
    List<RiderProfile> findByStatusAndUserDeletedAtIsNull(RiderStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<RiderProfile> findByAvailabilityAndUserDeletedAtIsNull(Boolean availability);

    @EntityGraph(attributePaths = {"user"})
    List<RiderProfile> findByStatusAndAvailabilityAndUserDeletedAtIsNull(RiderStatus status, Boolean availability);
}
