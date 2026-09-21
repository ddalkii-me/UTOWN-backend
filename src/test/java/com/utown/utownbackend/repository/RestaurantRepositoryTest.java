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
class RestaurantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private User owner;
    private City seoul;
    private City busan;
    private RestaurantType type;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("owner_repo@test.com");
        owner.setPhone("01088888888");
        owner.setName("Owner Repo");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        seoul = new City();
        seoul.setName("Seoul");
        entityManager.persist(seoul);

        busan = new City();
        busan.setName("Busan");
        entityManager.persist(busan);

        type = new RestaurantType();
        type.setName("Korean");
        entityManager.persist(type);

        entityManager.flush();
    }

    private Restaurant createRestaurant(String name, City city, LocalDateTime deletedAt) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setAddress("123 Street");
        restaurant.setPhone("02-123-4567");
        restaurant.setMinimumOrderAmount(new BigDecimal("10000"));
        restaurant.setDeletedAt(deletedAt);
        return entityManager.persist(restaurant);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns only active restaurants")
    void findAllByDeletedAtIsNull_success() {
        createRestaurant("Active 1", seoul, null);
        createRestaurant("Active 2", busan, null);
        createRestaurant("Deleted", seoul, LocalDateTime.now());
        entityManager.flush();

        List<Restaurant> result = restaurantRepository.findAllByDeletedAtIsNull();

        assertThat(result).hasSize(2)
                .extracting(Restaurant::getName)
                .containsExactlyInAnyOrder("Active 1", "Active 2");
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - eagerly fetches owner, type, and city to eliminate N+1")
    void findAllByDeletedAtIsNull_eagerlyFetchesAssociations() {
        createRestaurant("Active 1", seoul, null);
        entityManager.flush();
        entityManager.clear();

        List<Restaurant> result = restaurantRepository.findAllByDeletedAtIsNull();

        assertThat(result).hasSize(1);
        assertThat(org.hibernate.Hibernate.isInitialized(result.get(0).getOwner())).isTrue();
        assertThat(org.hibernate.Hibernate.isInitialized(result.get(0).getType())).isTrue();
        assertThat(org.hibernate.Hibernate.isInitialized(result.get(0).getCity())).isTrue();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns restaurant if active, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        Restaurant active = createRestaurant("Seoul Bistro", seoul, null);
        Restaurant deleted = createRestaurant("Old Diner", seoul, LocalDateTime.now());
        entityManager.flush();

        Optional<Restaurant> foundActive = restaurantRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<Restaurant> foundDeleted = restaurantRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Seoul Bistro");
        assertThat(foundDeleted).isEmpty();
    }

    @Test
    @DisplayName("existsByCityIdAndDeletedAtIsNull - checks active restaurant existence by city")
    void existsByCityIdAndDeletedAtIsNull_success() {
        createRestaurant("Seoul Grill", seoul, null);
        createRestaurant("Busan Former Grill", busan, LocalDateTime.now());
        entityManager.flush();

        assertThat(restaurantRepository.existsByCityIdAndDeletedAtIsNull(seoul.getId())).isTrue();
        assertThat(restaurantRepository.existsByCityIdAndDeletedAtIsNull(busan.getId())).isFalse();
        assertThat(restaurantRepository.existsByCityIdAndDeletedAtIsNull(999L)).isFalse();
    }

    @Test
    @DisplayName("save - populates and updates JPA auditing timestamps createdAt and updatedAt")
    void save_shouldPopulateAuditingTimestamps() {
        Restaurant restaurant = createRestaurant("Audit Cafe", seoul, null);
        entityManager.flush();

        assertThat(restaurant.getCreatedAt()).isNotNull();
        assertThat(restaurant.getUpdatedAt()).isNotNull();

        LocalDateTime initialUpdatedAt = restaurant.getUpdatedAt();
        restaurant.setDescription("Updated description");
        restaurantRepository.save(restaurant);
        entityManager.flush();

        assertThat(restaurant.getUpdatedAt()).isNotNull();
    }
}
