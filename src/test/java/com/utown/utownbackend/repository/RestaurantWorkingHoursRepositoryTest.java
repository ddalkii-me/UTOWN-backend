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
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RestaurantWorkingHoursRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RestaurantWorkingHoursRepository workingHoursRepository;

    private Restaurant restaurant1;
    private Restaurant restaurant2;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("hours_owner@test.com");
        owner.setPhone("01033333333");
        owner.setName("Hours Owner");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Seoul");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Cafe");
        entityManager.persist(type);

        restaurant1 = new Restaurant();
        restaurant1.setName("Cafe Morning");
        restaurant1.setCity(city);
        restaurant1.setType(type);
        restaurant1.setOwner(owner);
        restaurant1.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant1);

        restaurant2 = new Restaurant();
        restaurant2.setName("Cafe Evening");
        restaurant2.setCity(city);
        restaurant2.setType(type);
        restaurant2.setOwner(owner);
        restaurant2.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant2);

        entityManager.flush();
    }

    private RestaurantWorkingHours createWorkingHours(Restaurant restaurant, DayOfWeek day, LocalTime open, LocalTime close, boolean isDayOff) {
        RestaurantWorkingHours hours = new RestaurantWorkingHours();
        hours.setRestaurant(restaurant);
        hours.setDayOfWeek(day);
        hours.setOpenTime(open);
        hours.setCloseTime(close);
        hours.setDayOff(isDayOff);
        return entityManager.persist(hours);
    }

    @Test
    @DisplayName("findByRestaurantIdAndDayOfWeek - returns working hours for specific restaurant and day")
    void findByRestaurantIdAndDayOfWeek_success() {
        createWorkingHours(restaurant1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(21, 0), false);
        createWorkingHours(restaurant1, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(21, 0), false);
        createWorkingHours(restaurant2, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false);
        entityManager.flush();

        Optional<RestaurantWorkingHours> found = workingHoursRepository.findByRestaurantIdAndDayOfWeek(restaurant1.getId(), DayOfWeek.MONDAY);

        assertThat(found).isPresent();
        assertThat(found.get().getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(found.get().getCloseTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(found.get().isDayOff()).isFalse();

        Optional<RestaurantWorkingHours> notFound = workingHoursRepository.findByRestaurantIdAndDayOfWeek(restaurant1.getId(), DayOfWeek.SUNDAY);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("findByRestaurantId - returns all working hours for restaurant")
    void findByRestaurantId_success() {
        createWorkingHours(restaurant1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(21, 0), false);
        createWorkingHours(restaurant1, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(21, 0), false);
        createWorkingHours(restaurant1, DayOfWeek.WEDNESDAY, null, null, true);
        createWorkingHours(restaurant2, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(22, 0), false);
        entityManager.flush();

        List<RestaurantWorkingHours> hoursList = workingHoursRepository.findByRestaurantId(restaurant1.getId());

        assertThat(hoursList).hasSize(3);
        assertThat(hoursList).extracting(RestaurantWorkingHours::getDayOfWeek)
                .containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
    }
}
