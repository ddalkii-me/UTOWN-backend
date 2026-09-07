package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.RestaurantType;
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
class RestaurantTypeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RestaurantTypeRepository restaurantTypeRepository;

    @Test
    @DisplayName("save - populates JPA auditing timestamps createdAt and updatedAt")
    void save_shouldPopulateAuditingTimestamps() {
        RestaurantType type = new RestaurantType();
        type.setName("Italian");
        type.setDescription("Pasta and pizza");

        RestaurantType saved = restaurantTypeRepository.save(type);
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findById - returns restaurant type when present")
    void findById_success() {
        RestaurantType type = new RestaurantType();
        type.setName("Mexican");
        type.setDescription("Tacos and burritos");
        entityManager.persist(type);
        entityManager.flush();

        Optional<RestaurantType> found = restaurantTypeRepository.findById(type.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Mexican");
    }

    @Test
    @DisplayName("findAll - returns all restaurant types")
    void findAll_success() {
        RestaurantType type1 = new RestaurantType();
        type1.setName("Korean");
        entityManager.persist(type1);

        RestaurantType type2 = new RestaurantType();
        type2.setName("Japanese");
        entityManager.persist(type2);
        entityManager.flush();

        List<RestaurantType> allTypes = restaurantTypeRepository.findAll();

        assertThat(allTypes).extracting(RestaurantType::getName)
                .contains("Korean", "Japanese");
    }

    @Test
    @DisplayName("delete - removes restaurant type")
    void delete_success() {
        RestaurantType type = new RestaurantType();
        type.setName("Fast Food");
        entityManager.persist(type);
        entityManager.flush();

        restaurantTypeRepository.delete(type);
        entityManager.flush();

        assertThat(restaurantTypeRepository.findById(type.getId())).isEmpty();
    }
}
