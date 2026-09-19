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
class NotificationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    private User user1;
    private User user2;
    private Notification n1;
    private Notification n2;
    private Notification n3;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setEmail("user1@test.com");
        user1.setPhone("01011112222");
        user1.setName("User One");
        user1.setPassword("pwd123456");
        user1.setRole(UserRole.CUSTOMER);
        user1.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user1);

        user2 = new User();
        user2.setEmail("user2@test.com");
        user2.setPhone("01033334444");
        user2.setName("User Two");
        user2.setPassword("pwd123456");
        user2.setRole(UserRole.CUSTOMER);
        user2.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user2);

        n1 = new Notification();
        n1.setUser(user1);
        n1.setType(NotificationType.ORDER_STATUS_CHANGED);
        n1.setTitle("Order Accepted");
        n1.setMessage("Your order has been accepted");
        n1.setIsRead(false);
        entityManager.persist(n1);

        n2 = new Notification();
        n2.setUser(user1);
        n2.setType(NotificationType.SYSTEM);
        n2.setTitle("Welcome");
        n2.setMessage("Welcome to UTown");
        n2.setIsRead(true);
        entityManager.persist(n2);

        n3 = new Notification();
        n3.setUser(user2);
        n3.setType(NotificationType.NEW_ORDER);
        n3.setTitle("New Order");
        n3.setMessage("A new order was placed");
        n3.setIsRead(false);
        entityManager.persist(n3);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByUserIdOrderByCreatedAtDesc - should return notifications for user in desc order")
    void findByUserIdOrderByCreatedAtDesc_shouldReturnList() {
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(user1.getId());
        assertThat(list).hasSize(2);
        assertThat(list).extracting(Notification::getUser).extracting(User::getId)
                .containsOnly(user1.getId());
    }

    @Test
    @DisplayName("findByUserIdAndIsReadOrderByCreatedAtDesc - should filter by read status")
    void findByUserIdAndIsReadOrderByCreatedAtDesc_shouldFilter() {
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user1.getId(), false);
        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).getTitle()).isEqualTo("Order Accepted");

        List<Notification> read = notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(user1.getId(), true);
        assertThat(read).hasSize(1);
        assertThat(read.get(0).getTitle()).isEqualTo("Welcome");
    }

    @Test
    @DisplayName("countByUserIdAndIsReadFalse - should count unread notifications")
    void countByUserIdAndIsReadFalse_shouldReturnCount() {
        long count1 = notificationRepository.countByUserIdAndIsReadFalse(user1.getId());
        long count2 = notificationRepository.countByUserIdAndIsReadFalse(user2.getId());

        assertThat(count1).isEqualTo(1L);
        assertThat(count2).isEqualTo(1L);
    }

    @Test
    @DisplayName("findByIdAndUserId - should find notification only when userId matches")
    void findByIdAndUserId_shouldEnforceUserOwnership() {
        Optional<Notification> found = notificationRepository.findByIdAndUserId(n1.getId(), user1.getId());
        assertThat(found).isPresent();

        Optional<Notification> wrongUser = notificationRepository.findByIdAndUserId(n1.getId(), user2.getId());
        assertThat(wrongUser).isEmpty();
    }

    @Test
    @DisplayName("markAllAsReadByUserId - should mark all unread as read for user")
    void markAllAsReadByUserId_shouldUpdateInBulk() {
        int updated = notificationRepository.markAllAsReadByUserId(user1.getId());
        assertThat(updated).isEqualTo(1);

        entityManager.clear();

        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user1.getId());
        assertThat(unreadCount).isEqualTo(0L);

        // user2 unread should remain untouched
        long user2Unread = notificationRepository.countByUserIdAndIsReadFalse(user2.getId());
        assertThat(user2Unread).isEqualTo(1L);
    }
}
