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
class DishOptionGroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DishOptionGroupRepository dishOptionGroupRepository;

    private Dish dish1;
    private Dish dish2;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@example.com");
        owner.setUsername("owner1");
        owner.setName("Owner One");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Seoul");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Korean");
        entityManager.persist(type);

        Restaurant restaurant = new Restaurant();
        restaurant.setName("Seoul Kitchen");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        Category category = new Category();
        category.setName("Main");
        category.setRestaurant(restaurant);
        category.setPriority(1);
        entityManager.persist(category);

        dish1 = new Dish();
        dish1.setName("Bibimbap");
        dish1.setRestaurant(restaurant);
        dish1.setCategory(category);
        dish1.setPrice(new BigDecimal("10000"));
        dish1.setStatus(DishStatus.AVAILABLE);
        dish1.setSortOrder(1);
        entityManager.persist(dish1);

        dish2 = new Dish();
        dish2.setName("Bulgogi");
        dish2.setRestaurant(restaurant);
        dish2.setCategory(category);
        dish2.setPrice(new BigDecimal("15000"));
        dish2.setStatus(DishStatus.AVAILABLE);
        dish2.setSortOrder(2);
        entityManager.persist(dish2);

        entityManager.flush();
    }

    private DishOptionGroup createGroup(String name, Dish dish, int sortOrder, LocalDateTime deletedAt) {
        DishOptionGroup group = new DishOptionGroup();
        group.setName(name);
        group.setDish(dish);
        group.setRequired(false);
        group.setMinSelections(0);
        group.setMaxSelections(3);
        group.setSortOrder(sortOrder);
        group.setDeletedAt(deletedAt);
        return entityManager.persist(group);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNullOrderBySortOrderAsc - returns only active groups ordered by sortOrder")
    void findAllByDeletedAtIsNullOrderBySortOrderAsc_success() {
        createGroup("Group B", dish1, 2, null);
        createGroup("Group A", dish1, 1, null);
        createGroup("Group Deleted", dish1, 0, LocalDateTime.now());

        List<DishOptionGroup> result = dishOptionGroupRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Group A");
        assertThat(result.get(1).getName()).isEqualTo("Group B");
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNullOrderBySortOrderAsc - excludes groups if parent dish is soft deleted")
    void findAllByDeletedAtIsNullOrderBySortOrderAsc_excludesDeletedDishGroups() {
        createGroup("Group for Active Dish", dish1, 1, null);
        createGroup("Group for Deleted Dish", dish2, 2, null);

        dish2.setDeletedAt(LocalDateTime.now());
        entityManager.persist(dish2);
        entityManager.flush();

        List<DishOptionGroup> result = dishOptionGroupRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Group for Active Dish");
    }

    @Test
    @DisplayName("findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc - filters by dish and excludes deleted")
    void findAllByDishId_filtersProperly() {
        createGroup("Dish1 - Group 2", dish1, 2, null);
        createGroup("Dish1 - Group 1", dish1, 1, null);
        createGroup("Dish1 - Group Deleted", dish1, 0, LocalDateTime.now());
        createGroup("Dish2 - Group 1", dish2, 1, null);

        List<DishOptionGroup> result = dishOptionGroupRepository
                .findAllByDishIdAndDeletedAtIsNullOrderBySortOrderAsc(dish1.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Dish1 - Group 1");
        assertThat(result.get(1).getName()).isEqualTo("Dish1 - Group 2");
    }

    @Test
    @DisplayName("existsByDishIdAndDeletedAtIsNull - returns true only when active group exists")
    void existsByDishIdAndDeletedAtIsNull_success() {
        createGroup("Active Group", dish1, 1, null);
        createGroup("Deleted Group", dish2, 1, LocalDateTime.now());

        assertThat(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(dish1.getId())).isTrue();
        assertThat(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(dish2.getId())).isFalse();
        assertThat(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(999L)).isFalse();
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns group if active, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        DishOptionGroup active = createGroup("Active", dish1, 1, null);
        DishOptionGroup deleted = createGroup("Deleted", dish1, 2, LocalDateTime.now());

        Optional<DishOptionGroup> activeResult = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<DishOptionGroup> deletedResult = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(activeResult).isPresent();
        assertThat(activeResult.get().getName()).isEqualTo("Active");
        assertThat(deletedResult).isEmpty();
    }
}
