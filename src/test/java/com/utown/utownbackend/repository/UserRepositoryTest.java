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

    private User createUser(String email, String username, LocalDateTime deletedAt) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
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
        User active = createUser("active@test.com", "active_u", null);
        User deleted = createUser("deleted@test.com", "deleted_u", LocalDateTime.now());
        entityManager.flush();

        Optional<User> foundActive = userRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<User> foundDeleted = userRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getEmail()).isEqualTo("active@test.com");
        assertThat(foundDeleted).isEmpty();
    }
}
