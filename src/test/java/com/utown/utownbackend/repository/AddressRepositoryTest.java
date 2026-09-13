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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AddressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AddressRepository addressRepository;

    private User user1;
    private User user2;
    private City city;
    private DeliveryArea deliveryArea1;
    private DeliveryArea deliveryArea2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setEmail("user1@test.com");
        user1.setPhone("01011111111");
        user1.setName("User One");
        user1.setPassword("pwd1");
        user1.setRole(UserRole.CUSTOMER);
        user1.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user1);

        user2 = new User();
        user2.setEmail("user2@test.com");
        user2.setPhone("01022222222");
        user2.setName("User Two");
        user2.setPassword("pwd2");
        user2.setRole(UserRole.CUSTOMER);
        user2.setStatus(UserStatus.ACTIVE);
        entityManager.persist(user2);

        city = new City();
        city.setName("Seoul");
        entityManager.persist(city);

        deliveryArea1 = new DeliveryArea();
        deliveryArea1.setName("Gangnam");
        deliveryArea1.setCity(city);
        entityManager.persist(deliveryArea1);

        deliveryArea2 = new DeliveryArea();
        deliveryArea2.setName("Seocho");
        deliveryArea2.setCity(city);
        entityManager.persist(deliveryArea2);

        entityManager.flush();
    }

    private Address createAddress(User user, DeliveryArea area, String label, LocalDateTime deletedAt) {
        Address address = new Address();
        address.setUser(user);
        address.setCity(city);
        address.setDeliveryArea(area);
        address.setLabel(label);
        address.setRecipientName("Recipient " + label);
        address.setPhone("010-1234-5678");
        address.setAddressLine("123 Street " + label);
        address.setPostalCode("12345");
        address.setLatitude(new BigDecimal("37.4979"));
        address.setLongitude(new BigDecimal("127.0276"));
        address.setDeletedAt(deletedAt);
        return entityManager.persist(address);
    }

    @Test
    @DisplayName("findAllByUserIdAndDeletedAtIsNull - returns only active addresses of user")
    void findAllByUserId_success() {
        createAddress(user1, deliveryArea1, "Home", null);
        createAddress(user1, deliveryArea1, "Work", null);
        createAddress(user1, deliveryArea1, "Deleted", LocalDateTime.now());
        createAddress(user2, deliveryArea2, "User2 Home", null);

        List<Address> result = addressRepository.findAllByUserIdAndDeletedAtIsNull(user1.getId());

        assertThat(result).hasSize(2)
                .extracting(Address::getLabel)
                .containsExactlyInAnyOrder("Home", "Work");
    }

    @Test
    @DisplayName("existsByDeliveryAreaIdAndDeletedAtIsNull - checks active address presence in area")
    void existsByDeliveryAreaId_success() {
        createAddress(user1, deliveryArea1, "Home", null);
        createAddress(user1, deliveryArea2, "Old Area", LocalDateTime.now());

        assertThat(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(deliveryArea1.getId())).isTrue();
        assertThat(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(deliveryArea2.getId())).isFalse();
        assertThat(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(999L)).isFalse();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns address when not deleted")
    void findByIdAndDeletedAtIsNull_success() {
        Address active = createAddress(user1, deliveryArea1, "Active", null);
        Address deleted = createAddress(user1, deliveryArea1, "Deleted", LocalDateTime.now());

        Optional<Address> foundActive = addressRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<Address> foundDeleted = addressRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getLabel()).isEqualTo("Active");
        assertThat(foundDeleted).isEmpty();
    }
}
