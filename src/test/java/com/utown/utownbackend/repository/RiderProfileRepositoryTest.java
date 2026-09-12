package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RiderProfileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RiderProfileRepository riderProfileRepository;

    private User user1;
    private User user2;
    private User user3;
    private RiderProfile rider1;
    private RiderProfile rider2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setEmail("rider1@test.com");
        user1.setPhone("01011112222");
        user1.setName("Rider One");
        user1.setPassword("pwd123456");
        user1.setRole(UserRole.RIDER);
        user1.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user1);

        user2 = new User();
        user2.setEmail("rider2@test.com");
        user2.setPhone("01033334444");
        user2.setName("Rider Two");
        user2.setPassword("pwd123456");
        user2.setRole(UserRole.RIDER);
        user2.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user2);

        user3 = new User();
        user3.setEmail("rider3@test.com");
        user3.setPhone("01055556666");
        user3.setName("Rider Three");
        user3.setPassword("pwd123456");
        user3.setRole(UserRole.RIDER);
        user3.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user3);

        rider1 = new RiderProfile();
        rider1.setUser(user1);
        rider1.setTransportType(TransportType.MOTORCYCLE);
        rider1.setAvailability(true);
        rider1.setStatus(RiderStatus.ACTIVE);
        entityManager.persist(rider1);

        rider2 = new RiderProfile();
        rider2.setUser(user2);
        rider2.setTransportType(TransportType.BICYCLE);
        rider2.setAvailability(false);
        rider2.setStatus(RiderStatus.INACTIVE);
        entityManager.persist(rider2);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByUserId - should return rider profile when user exists")
    void findByUserId_shouldReturnRiderProfile() {
        Optional<RiderProfile> found = riderProfileRepository.findByUserId(user1.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user1.getId());
        assertThat(found.get().getTransportType()).isEqualTo(TransportType.MOTORCYCLE);
    }

    @Test
    @DisplayName("findByUserId - should return empty when user has no rider profile")
    void findByUserId_shouldReturnEmpty() {
        Optional<RiderProfile> found = riderProfileRepository.findByUserId(user3.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByUserId - should return true when profile exists and false otherwise")
    void existsByUserId_shouldCheckExistence() {
        assertThat(riderProfileRepository.existsByUserId(user1.getId())).isTrue();
        assertThat(riderProfileRepository.existsByUserId(user3.getId())).isFalse();
    }

    @Test
    @DisplayName("findByStatus - should filter rider profiles by status")
    void findByStatus_shouldFilterCorrectly() {
        List<RiderProfile> activeRiders = riderProfileRepository.findByStatus(RiderStatus.ACTIVE);
        List<RiderProfile> inactiveRiders = riderProfileRepository.findByStatus(RiderStatus.INACTIVE);

        assertThat(activeRiders).hasSize(1);
        assertThat(activeRiders.get(0).getUser().getEmail()).isEqualTo("rider1@test.com");
        assertThat(inactiveRiders).hasSize(1);
        assertThat(inactiveRiders.get(0).getUser().getEmail()).isEqualTo("rider2@test.com");
    }

    @Test
    @DisplayName("findByAvailability - should filter rider profiles by availability")
    void findByAvailability_shouldFilterCorrectly() {
        List<RiderProfile> availableRiders = riderProfileRepository.findByAvailability(true);
        List<RiderProfile> unavailableRiders = riderProfileRepository.findByAvailability(false);

        assertThat(availableRiders).hasSize(1);
        assertThat(availableRiders.get(0).getUser().getEmail()).isEqualTo("rider1@test.com");
        assertThat(unavailableRiders).hasSize(1);
        assertThat(unavailableRiders.get(0).getUser().getEmail()).isEqualTo("rider2@test.com");
    }

    @Test
    @DisplayName("findByStatusAndAvailability - should match both criteria")
    void findByStatusAndAvailability_shouldFilterCorrectly() {
        List<RiderProfile> results = riderProfileRepository.findByStatusAndAvailability(RiderStatus.ACTIVE, true);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUser().getId()).isEqualTo(user1.getId());

        List<RiderProfile> none = riderProfileRepository.findByStatusAndAvailability(RiderStatus.ACTIVE, false);
        assertThat(none).isEmpty();
    }
}
