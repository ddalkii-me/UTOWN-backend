package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.AuthCode;
import com.utown.utownbackend.entity.AuthCodePurpose;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
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
class AuthCodeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthCodeRepository authCodeRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setPhone("+821012345678");
        user.setName("Test User");
        user.setPassword("secret");
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user = entityManager.persist(user);

        otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setPhone("+821098765432");
        otherUser.setName("Other User");
        otherUser.setPassword("secret");
        otherUser.setRole(UserRole.CUSTOMER);
        otherUser.setStatus(UserStatus.ACTIVE);
        otherUser = entityManager.persist(otherUser);

        entityManager.flush();
    }

    private AuthCode persistAuthCode(User u, String codeHash, AuthCodePurpose purpose, LocalDateTime createdAt, LocalDateTime usedAt) {
        AuthCode authCode = new AuthCode();
        authCode.setUser(u);
        authCode.setCodeHash(codeHash);
        authCode.setPurpose(purpose);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authCode.setUsedAt(usedAt);
        authCode.setCreatedAt(createdAt);
        authCode.setUpdatedAt(createdAt);
        return entityManager.persist(authCode);
    }

    @Test
    @DisplayName("findTopByUserAndPurposeOrderByCreatedAtDesc - returns most recent auth code for user")
    void findTopByUserAndPurposeOrderByCreatedAtDesc_returnsLatest() {
        LocalDateTime now = LocalDateTime.now();
        persistAuthCode(user, "hash1", AuthCodePurpose.PASSWORD_RESET, now.minusMinutes(2), null);
        AuthCode newerCode = persistAuthCode(user, "hash2", AuthCodePurpose.PASSWORD_RESET, now.minusSeconds(30), null);
        persistAuthCode(otherUser, "otherHash", AuthCodePurpose.PASSWORD_RESET, now, null);
        entityManager.flush();

        Optional<AuthCode> result = authCodeRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET);

        assertThat(result).isPresent();
        assertThat(result.get().getCodeHash()).isEqualTo("hash2");
        assertThat(result.get().getId()).isEqualTo(newerCode.getId());
    }

    @Test
    @DisplayName("findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc - returns latest unused auth code")
    void findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc_returnsLatestUnused() {
        LocalDateTime now = LocalDateTime.now();
        AuthCode unusedOld = persistAuthCode(user, "hashOldUnused", AuthCodePurpose.PASSWORD_RESET, now.minusMinutes(5), null);
        persistAuthCode(user, "hashNewerUsed", AuthCodePurpose.PASSWORD_RESET, now.minusMinutes(1), now.minusMinutes(1));
        entityManager.flush();

        Optional<AuthCode> result = authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET);

        assertThat(result).isPresent();
        assertThat(result.get().getCodeHash()).isEqualTo("hashOldUnused");
        assertThat(result.get().getId()).isEqualTo(unusedOld.getId());
    }

    @Test
    @DisplayName("findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc - returns empty when all codes are used")
    void findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc_allUsedReturnsEmpty() {
        LocalDateTime now = LocalDateTime.now();
        persistAuthCode(user, "hashUsed", AuthCodePurpose.PASSWORD_RESET, now.minusMinutes(1), now);
        entityManager.flush();

        Optional<AuthCode> result = authCodeRepository.findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user, AuthCodePurpose.PASSWORD_RESET);

        assertThat(result).isEmpty();
    }
}
