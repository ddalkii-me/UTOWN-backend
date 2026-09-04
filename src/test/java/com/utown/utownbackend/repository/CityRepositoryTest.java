package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.City;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CityRepository cityRepository;

    private City createCity(String name, LocalDateTime deletedAt) {
        City city = new City();
        city.setName(name);
        city.setDeletedAt(deletedAt);
        return entityManager.persist(city);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns only active cities")
    void findAllByDeletedAtIsNull_success() {
        createCity("Seoul", null);
        createCity("Busan", null);
        createCity("Old City", LocalDateTime.now());

        List<City> cities = cityRepository.findAllByDeletedAtIsNull();

        assertThat(cities).hasSize(2)
                .extracting(City::getName)
                .containsExactlyInAnyOrder("Seoul", "Busan");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns city if active, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        City active = createCity("Daegu", null);
        City deleted = createCity("Gwangju", LocalDateTime.now());

        Optional<City> activeFound = cityRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<City> deletedFound = cityRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(activeFound).isPresent();
        assertThat(activeFound.get().getName()).isEqualTo("Daegu");
        assertThat(deletedFound).isEmpty();
    }

    @Test
    @DisplayName("existsByName - checks name existence correctly")
    void existsByName_success() {
        createCity("Daejeon", null);

        assertThat(cityRepository.existsByName("Daejeon")).isTrue();
        assertThat(cityRepository.existsByName("Ulsan")).isFalse();
    }

    @Test
    @DisplayName("existsByNameAndIdNot - checks duplicate name excluding specific city id")
    void existsByNameAndIdNot_success() {
        City city1 = createCity("Suwon", null);
        City city2 = createCity("Incheon", null);

        // Name "Suwon" exists on city1, so checking against city2 should return true
        assertThat(cityRepository.existsByNameAndIdNot("Suwon", city2.getId())).isTrue();

        // Checking against its own ID should return false (allows updating own name)
        assertThat(cityRepository.existsByNameAndIdNot("Suwon", city1.getId())).isFalse();

        // Non-existent name should return false
        assertThat(cityRepository.existsByNameAndIdNot("NonExistent", city1.getId())).isFalse();
    }
}
