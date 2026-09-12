package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User createUser(String email, String phone, LocalDateTime deletedAt) {
        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setName("Test User");
        user.setPassword("secret");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeletedAt(deletedAt);
        return entityManager.persist(user);
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns user if active, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        User active = createUser("active@test.com", "01011112222", null);
        User deleted = createUser("deleted@test.com", "01033334444", LocalDateTime.now());
        entityManager.flush();

        Optional<User> foundActive = userRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<User> foundDeleted = userRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getEmail()).isEqualTo("active@test.com");
        assertThat(foundDeleted).isEmpty();
    }

    @Test
    @DisplayName("findByPhoneAndDeletedAtIsNull - returns user if active, empty if deleted")
    void findByPhoneAndDeletedAtIsNull_success() {
        User active = createUser("active2@test.com", "01055556666", null);
        User deleted = createUser("deleted2@test.com", "01077778888", LocalDateTime.now());
        entityManager.flush();

        Optional<User> foundActive = userRepository.findByPhoneAndDeletedAtIsNull("01055556666");
        Optional<User> foundDeleted = userRepository.findByPhoneAndDeletedAtIsNull("01077778888");

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getPhone()).isEqualTo("01055556666");
        assertThat(foundDeleted).isEmpty();
    }

    @Test
    @DisplayName("findByEmailAndDeletedAtIsNull - returns user if active, empty if deleted")
    void findByEmailAndDeletedAtIsNull_success() {
        User active = createUser("findme@test.com", "01088889999", null);
        User deleted = createUser("findme_del@test.com", "01088880000", LocalDateTime.now());
        entityManager.flush();

        Optional<User> foundActive = userRepository.findByEmailAndDeletedAtIsNull("findme@test.com");
        Optional<User> foundDeleted = userRepository.findByEmailAndDeletedAtIsNull("findme_del@test.com");

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getEmail()).isEqualTo("findme@test.com");
        assertThat(foundDeleted).isEmpty();
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns only non-deleted users")
    void findAllByDeletedAtIsNull_success() {
        createUser("all1@test.com", "01012340001", null);
        createUser("all2@test.com", "01012340002", null);
        createUser("all3@test.com", "01012340003", LocalDateTime.now());
        entityManager.flush();

        java.util.List<User> nonDeleted = userRepository.findAllByDeletedAtIsNull();

        assertThat(nonDeleted).isNotEmpty();
        assertThat(nonDeleted).allMatch(u -> u.getDeletedAt() == null);
        assertThat(nonDeleted).extracting(User::getEmail).doesNotContain("all3@test.com");
    }

    @Test
    @DisplayName("findByRoleAndDeletedAtIsNull - filters users by role")
    void findByRoleAndDeletedAtIsNull_success() {
        User rider = createUser("rider1@test.com", "01012340011", null);
        rider.setRole(UserRole.RIDER);
        entityManager.persist(rider);

        User customer = createUser("cust1@test.com", "01012340012", null);
        customer.setRole(UserRole.CUSTOMER);
        entityManager.persist(customer);
        entityManager.flush();

        java.util.List<User> riders = userRepository.findByRoleAndDeletedAtIsNull(UserRole.RIDER);

        assertThat(riders).isNotEmpty();
        assertThat(riders).allMatch(u -> u.getRole() == UserRole.RIDER && u.getDeletedAt() == null);
    }

    @Test
    @DisplayName("findByStatusAndDeletedAtIsNull - filters users by status")
    void findByStatusAndDeletedAtIsNull_success() {
        User suspended = createUser("susp1@test.com", "01012340021", null);
        suspended.setStatus(UserStatus.SUSPENDED);
        entityManager.persist(suspended);

        User active = createUser("act1@test.com", "01012340022", null);
        active.setStatus(UserStatus.ACTIVE);
        entityManager.persist(active);
        entityManager.flush();

        java.util.List<User> suspendedUsers = userRepository.findByStatusAndDeletedAtIsNull(UserStatus.SUSPENDED);

        assertThat(suspendedUsers).isNotEmpty();
        assertThat(suspendedUsers).allMatch(u -> u.getStatus() == UserStatus.SUSPENDED && u.getDeletedAt() == null);
    }

    @Test
    @DisplayName("findByRoleAndStatusAndDeletedAtIsNull - filters by role and status")
    void findByRoleAndStatusAndDeletedAtIsNull_success() {
        User activeOwner = createUser("owner1@test.com", "01012340031", null);
        activeOwner.setRole(UserRole.RESTAURANT_OWNER);
        activeOwner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(activeOwner);

        User suspendedOwner = createUser("owner2@test.com", "01012340032", null);
        suspendedOwner.setRole(UserRole.RESTAURANT_OWNER);
        suspendedOwner.setStatus(UserStatus.SUSPENDED);
        entityManager.persist(suspendedOwner);
        entityManager.flush();

        java.util.List<User> result = userRepository.findByRoleAndStatusAndDeletedAtIsNull(UserRole.RESTAURANT_OWNER, UserStatus.ACTIVE);

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(u -> u.getRole() == UserRole.RESTAURANT_OWNER && u.getStatus() == UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("existsByIdAndDeletedAtIsNull - returns true for active user, false for deleted or non-existent")
    void existsByIdAndDeletedAtIsNull_success() {
        User active = createUser("exists1@test.com", "01012340041", null);
        User deleted = createUser("exists2@test.com", "01012340042", LocalDateTime.now());
        entityManager.flush();

        assertThat(userRepository.existsByIdAndDeletedAtIsNull(active.getId())).isTrue();
        assertThat(userRepository.existsByIdAndDeletedAtIsNull(deleted.getId())).isFalse();
        assertThat(userRepository.existsByIdAndDeletedAtIsNull(999999L)).isFalse();
    }
}
