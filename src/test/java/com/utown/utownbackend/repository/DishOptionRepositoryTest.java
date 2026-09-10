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
class DishOptionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DishOptionRepository dishOptionRepository;

    private Dish activeDish;
    private Dish deletedDish;
    private DishOptionGroup activeGroup;
    private DishOptionGroup deletedGroup;
    private DishOptionGroup groupOnDeletedDish;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setPhone("01077777777");
        owner.setName("Owner Option");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Busan");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Japanese");
        entityManager.persist(type);

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Sushi Bar");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        Category category = new Category();
        category.setName("Rolls");
        category.setRestaurant(restaurant);
        category.setPriority(1);
        entityManager.persist(category);

        activeDish = new Dish();
        activeDish.setName("California Roll");
        activeDish.setRestaurant(restaurant);
        activeDish.setCategory(category);
        activeDish.setPrice(new BigDecimal("10000"));
        activeDish.setStatus(DishStatus.AVAILABLE);
        entityManager.persist(activeDish);

        deletedDish = new Dish();
        deletedDish.setName("Old Roll");
        deletedDish.setRestaurant(restaurant);
        deletedDish.setCategory(category);
        deletedDish.setPrice(new BigDecimal("8000"));
        deletedDish.setStatus(DishStatus.AVAILABLE);
        deletedDish.setDeletedAt(LocalDateTime.now());
        entityManager.persist(deletedDish);

        activeGroup = new DishOptionGroup();
        activeGroup.setName("Size");
        activeGroup.setDish(activeDish);
        activeGroup.setRequired(true);
        activeGroup.setMinSelections(1);
        activeGroup.setMaxSelections(1);
        activeGroup.setSortOrder(1);
        entityManager.persist(activeGroup);

        deletedGroup = new DishOptionGroup();
        deletedGroup.setName("Spice Level");
        deletedGroup.setDish(activeDish);
        deletedGroup.setRequired(false);
        deletedGroup.setMinSelections(0);
        deletedGroup.setMaxSelections(1);
        deletedGroup.setSortOrder(2);
        deletedGroup.setDeletedAt(LocalDateTime.now());
        entityManager.persist(deletedGroup);

        groupOnDeletedDish = new DishOptionGroup();
        groupOnDeletedDish.setName("Topping");
        groupOnDeletedDish.setDish(deletedDish);
        groupOnDeletedDish.setRequired(false);
        groupOnDeletedDish.setMinSelections(0);
        groupOnDeletedDish.setMaxSelections(2);
        groupOnDeletedDish.setSortOrder(3);
        entityManager.persist(groupOnDeletedDish);

        entityManager.flush();
    }

    private DishOption createOption(String name, DishOptionGroup group, Integer sortOrder, LocalDateTime deletedAt) {
        DishOption option = new DishOption();
        option.setName(name);
        option.setOptionGroup(group);
        option.setAdditionalPrice(new BigDecimal("1000"));
        option.setSortOrder(sortOrder);
        option.setStatus(DishOptionStatus.AVAILABLE);
        option.setDeletedAt(deletedAt);
        return entityManager.persist(option);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNullOrderBySortOrderAsc - orders by sortOrder and excludes soft-deleted records")
    void findAllByDeletedAtIsNullOrderBySortOrderAsc_success() {
        createOption("Large", activeGroup, 2, null);
        createOption("Small", activeGroup, 1, null);
        createOption("Medium (Deleted)", activeGroup, 3, LocalDateTime.now());
        createOption("Mild (Group Deleted)", deletedGroup, 1, null);
        createOption("Sesame (Dish Deleted)", groupOnDeletedDish, 1, null);

        entityManager.flush();

        List<DishOption> results = dishOptionRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getName()).isEqualTo("Small");
        assertThat(results.get(1).getName()).isEqualTo("Large");
    }

    @Test
    @DisplayName("findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc - filters by group and orders properly")
    void findAllByOptionGroupId_success() {
        DishOption small = createOption("Small", activeGroup, 1, null);
        DishOption large = createOption("Large", activeGroup, 2, null);
        createOption("Deleted Size", activeGroup, 3, LocalDateTime.now());

        entityManager.flush();

        List<DishOption> results = dishOptionRepository.findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(activeGroup.getId());

        assertThat(results).hasSize(2);
        assertThat(results).extracting(DishOption::getName)
                .containsExactly("Small", "Large");
    }

    @Test
    @DisplayName("existsByOptionGroupIdAndDeletedAtIsNull - returns true if active option exists, false if only deleted")
    void existsByOptionGroupId_success() {
        createOption("Extra Sauce", activeGroup, 1, null);
        createOption("Deleted Spice", deletedGroup, 1, LocalDateTime.now());
        entityManager.flush();

        assertThat(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(activeGroup.getId())).isTrue();
        assertThat(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(deletedGroup.getId())).isFalse();
        assertThat(dishOptionRepository.existsByOptionGroupIdAndDeletedAtIsNull(999L)).isFalse();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns active option, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        DishOption active = createOption("Active Option", activeGroup, 1, null);
        DishOption deleted = createOption("Deleted Option", activeGroup, 2, LocalDateTime.now());
        entityManager.flush();

        Optional<DishOption> foundActive = dishOptionRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<DishOption> foundDeleted = dishOptionRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Active Option");
        assertThat(foundDeleted).isEmpty();
    }
}
