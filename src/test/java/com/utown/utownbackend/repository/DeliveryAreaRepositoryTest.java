package com.utown.utownbackend.repository;

import com.utown.utownbackend.config.JpaAuditingConfig;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.DeliveryArea;
import org.junit.jupiter.api.BeforeEach;
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
class DeliveryAreaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeliveryAreaRepository deliveryAreaRepository;

    private City seoul;
    private City busan;

    @BeforeEach
    void setUp() {
        seoul = new City();
        seoul.setName("Seoul");
        entityManager.persist(seoul);

        busan = new City();
        busan.setName("Busan");
        entityManager.persist(busan);

        entityManager.flush();
    }

    private DeliveryArea createDeliveryArea(String name, City city, LocalDateTime deletedAt) {
        DeliveryArea area = new DeliveryArea();
        area.setName(name);
        area.setCity(city);
        area.setDeletedAt(deletedAt);
        return entityManager.persist(area);
    }

    @Test
    @DisplayName("findAllByCityIdAndDeletedAtIsNull - returns only active areas for given city")
    void findAllByCityId_returnsProperly() {
        createDeliveryArea("Gangnam", seoul, null);
        createDeliveryArea("Mapo", seoul, null);
        createDeliveryArea("Deleted Area", seoul, LocalDateTime.now());
        createDeliveryArea("Haeundae", busan, null);

        List<DeliveryArea> result = deliveryAreaRepository.findAllByCityIdAndDeletedAtIsNull(seoul.getId());

        assertThat(result).hasSize(2)
                .extracting(DeliveryArea::getName)
                .containsExactlyInAnyOrder("Gangnam", "Mapo");
    }

    @Test
    @DisplayName("findByIdAndCityIdAndDeletedAtIsNull - returns area when city matches and not deleted")
    void findByIdAndCityId_success() {
        DeliveryArea gangnam = createDeliveryArea("Gangnam", seoul, null);

        Optional<DeliveryArea> found = deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(gangnam.getId(), seoul.getId());
        Optional<DeliveryArea> wrongCity = deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(gangnam.getId(), busan.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Gangnam");
        assertThat(wrongCity).isEmpty();
    }

    @Test
    @DisplayName("existsByCityIdAndNameAndDeletedAtIsNull - checks name uniqueness within city")
    void existsByCityIdAndName_success() {
        createDeliveryArea("Gangnam", seoul, null);
        createDeliveryArea("Deleted Area", seoul, LocalDateTime.now());

        assertThat(deliveryAreaRepository.existsByCityIdAndNameAndDeletedAtIsNull(seoul.getId(), "Gangnam")).isTrue();
        assertThat(deliveryAreaRepository.existsByCityIdAndNameAndDeletedAtIsNull(seoul.getId(), "Deleted Area")).isFalse();
        assertThat(deliveryAreaRepository.existsByCityIdAndNameAndDeletedAtIsNull(busan.getId(), "Gangnam")).isFalse();
    }

    @Test
    @DisplayName("existsByCityIdAndNameAndIdNotAndDeletedAtIsNull - checks duplicate name excluding area id")
    void existsByCityIdAndNameAndIdNot_success() {
        DeliveryArea area1 = createDeliveryArea("Gangnam", seoul, null);
        DeliveryArea area2 = createDeliveryArea("Mapo", seoul, null);

        // Checking if "Gangnam" exists in seoul for area2 should be true
        assertThat(deliveryAreaRepository.existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(seoul.getId(), "Gangnam", area2.getId())).isTrue();

        // Checking if "Gangnam" exists in seoul for area1 itself should be false (own ID ignored)
        assertThat(deliveryAreaRepository.existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(seoul.getId(), "Gangnam", area1.getId())).isFalse();
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns all active delivery areas across all cities")
    void findAllByDeletedAtIsNull_success() {
        createDeliveryArea("Gangnam", seoul, null);
        createDeliveryArea("Haeundae", busan, null);
        createDeliveryArea("Old Area", seoul, LocalDateTime.now());

        List<DeliveryArea> result = deliveryAreaRepository.findAllByDeletedAtIsNull();

        assertThat(result).hasSize(2)
                .extracting(DeliveryArea::getName)
                .containsExactlyInAnyOrder("Gangnam", "Haeundae");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns area when active, empty when deleted")
    void findByIdAndDeletedAtIsNull_success() {
        DeliveryArea active = createDeliveryArea("Jongno", seoul, null);
        DeliveryArea deleted = createDeliveryArea("Old Jongno", seoul, LocalDateTime.now());

        Optional<DeliveryArea> foundActive = deliveryAreaRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<DeliveryArea> foundDeleted = deliveryAreaRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Jongno");
        assertThat(foundDeleted).isEmpty();
    }
}
