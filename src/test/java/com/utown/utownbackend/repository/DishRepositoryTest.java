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
class DishRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DishRepository dishRepository;

    private Restaurant restaurant;
    private Category category;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setPhone("01044444444");
        owner.setName("Owner Two");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Incheon");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Italian");
        entityManager.persist(type);

        restaurant = new Restaurant();
        restaurant.setName("Pasta House");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        category = new Category();
        category.setName("Pasta");
        category.setRestaurant(restaurant);
        category.setPriority(1);
        entityManager.persist(category);

        entityManager.flush();
    }

    private Dish createDish(String name, DishStatus status, LocalDateTime deletedAt) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setPrice(new BigDecimal("12000"));
        dish.setStatus(status);
        dish.setSortOrder(1);
        dish.setDeletedAt(deletedAt);
        return entityManager.persist(dish);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns only non-deleted dishes")
    void findAllByDeletedAtIsNull_returnsOnlyActive() {
        createDish("Active Dish 1", DishStatus.AVAILABLE, null);
        createDish("Active Dish 2", DishStatus.ON_HOLD, null);
        createDish("Deleted Dish", DishStatus.AVAILABLE, LocalDateTime.now());

        List<Dish> result = dishRepository.findAllByDeletedAtIsNull();

        assertThat(result).hasSize(2)
                .extracting(Dish::getName)
                .containsExactlyInAnyOrder("Active Dish 1", "Active Dish 2");
    }

    @Test
    @DisplayName("findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc - eagerly fetches restaurant and category")
    void findAllByRestaurantId_eagerlyFetchesAssociations() {
        createDish("Active Dish 1", DishStatus.AVAILABLE, null);
        entityManager.flush();
        entityManager.clear();

        List<Dish> result = dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId());

        assertThat(result).hasSize(1);
        assertThat(org.hibernate.Hibernate.isInitialized(result.get(0).getRestaurant())).isTrue();
        assertThat(org.hibernate.Hibernate.isInitialized(result.get(0).getCategory())).isTrue();
    }

    @Test
    @DisplayName("findAllByStatusAndDeletedAtIsNull - filters by status and excludes deleted")
    void findAllByStatusAndDeletedAtIsNull_filtersProperly() {
        createDish("Available Dish", DishStatus.AVAILABLE, null);
        createDish("On Hold Dish", DishStatus.ON_HOLD, null);
        createDish("Deleted Available Dish", DishStatus.AVAILABLE, LocalDateTime.now());

        List<Dish> result = dishRepository.findAllByStatusAndDeletedAtIsNull(DishStatus.AVAILABLE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Available Dish");
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNotNull - returns only soft-deleted dishes")
    void findAllByDeletedAtIsNotNull_returnsDeleted() {
        createDish("Active Dish", DishStatus.AVAILABLE, null);
        createDish("Deleted Dish 1", DishStatus.AVAILABLE, LocalDateTime.now());
        createDish("Deleted Dish 2", DishStatus.ON_HOLD, LocalDateTime.now());

        List<Dish> result = dishRepository.findAllByDeletedAtIsNotNull();

        assertThat(result).hasSize(2)
                .extracting(Dish::getName)
                .containsExactlyInAnyOrder("Deleted Dish 1", "Deleted Dish 2");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull and findByIdAndDeletedAtIsNotNull - verify correct retrieval")
    void findById_softDeleteVerification() {
        Dish active = createDish("Active", DishStatus.AVAILABLE, null);
        Dish deleted = createDish("Deleted", DishStatus.AVAILABLE, LocalDateTime.now());

        assertThat(dishRepository.findByIdAndDeletedAtIsNull(active.getId())).isPresent();
        assertThat(dishRepository.findByIdAndDeletedAtIsNull(deleted.getId())).isEmpty();

        assertThat(dishRepository.findByIdAndDeletedAtIsNotNull(active.getId())).isEmpty();
        assertThat(dishRepository.findByIdAndDeletedAtIsNotNull(deleted.getId())).isPresent();
    }

    @Test
    @DisplayName("existsByCategoryIdAndDeletedAtIsNull - returns true only when active dishes exist for category")
    void existsByCategoryIdAndDeletedAtIsNull_success() {
        Category emptyCategory = new Category();
        emptyCategory.setName("Desserts");
        emptyCategory.setRestaurant(restaurant);
        emptyCategory.setPriority(2);
        entityManager.persist(emptyCategory);

        createDish("Active Pasta", DishStatus.AVAILABLE, null);

        assertThat(dishRepository.existsByCategoryIdAndDeletedAtIsNull(category.getId())).isTrue();
        assertThat(dishRepository.existsByCategoryIdAndDeletedAtIsNull(emptyCategory.getId())).isFalse();
    }

    @Test
    @DisplayName("findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc - returns dishes for target restaurant ordered by sortOrder")
    void findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc_success() {
        Dish d1 = new Dish();
        d1.setName("Dish 2");
        d1.setRestaurant(restaurant);
        d1.setCategory(category);
        d1.setPrice(new BigDecimal("10000"));
        d1.setStatus(DishStatus.AVAILABLE);
        d1.setSortOrder(2);
        entityManager.persist(d1);

        Dish d2 = new Dish();
        d2.setName("Dish 1");
        d2.setRestaurant(restaurant);
        d2.setCategory(category);
        d2.setPrice(new BigDecimal("8000"));
        d2.setStatus(DishStatus.AVAILABLE);
        d2.setSortOrder(1);
        entityManager.persist(d2);

        // Soft-deleted dish in same restaurant
        Dish dDeleted = new Dish();
        dDeleted.setName("Deleted");
        dDeleted.setRestaurant(restaurant);
        dDeleted.setCategory(category);
        dDeleted.setPrice(new BigDecimal("5000"));
        dDeleted.setStatus(DishStatus.AVAILABLE);
        dDeleted.setSortOrder(0);
        dDeleted.setDeletedAt(LocalDateTime.now());
        entityManager.persist(dDeleted);

        entityManager.flush();

        List<Dish> result = dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Dish 1");
        assertThat(result.get(1).getName()).isEqualTo("Dish 2");
    }

    @Test
    @DisplayName("findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc - filters by restaurant and category")
    void findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc_success() {
        Category secondCat = new Category();
        secondCat.setName("Pizza");
        secondCat.setRestaurant(restaurant);
        secondCat.setPriority(2);
        entityManager.persist(secondCat);

        Dish pastaDish = new Dish();
        pastaDish.setName("Spaghetti");
        pastaDish.setRestaurant(restaurant);
        pastaDish.setCategory(category);
        pastaDish.setPrice(new BigDecimal("11000"));
        pastaDish.setStatus(DishStatus.AVAILABLE);
        pastaDish.setSortOrder(1);
        entityManager.persist(pastaDish);

        Dish pizzaDish = new Dish();
        pizzaDish.setName("Margherita");
        pizzaDish.setRestaurant(restaurant);
        pizzaDish.setCategory(secondCat);
        pizzaDish.setPrice(new BigDecimal("15000"));
        pizzaDish.setStatus(DishStatus.AVAILABLE);
        pizzaDish.setSortOrder(1);
        entityManager.persist(pizzaDish);

        entityManager.flush();

        List<Dish> pastaList = dishRepository.findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId(), category.getId());
        assertThat(pastaList).hasSize(1);
        assertThat(pastaList.get(0).getName()).isEqualTo("Spaghetti");

        List<Dish> pizzaList = dishRepository.findAllByRestaurantIdAndCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId(), secondCat.getId());
        assertThat(pizzaList).hasSize(1);
        assertThat(pizzaList.get(0).getName()).isEqualTo("Margherita");
    }

    @Test
    @DisplayName("findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc - filters by restaurant and availability status")
    void findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc_success() {
        createDish("Available Dish", DishStatus.AVAILABLE, null);
        createDish("On Hold Dish", DishStatus.ON_HOLD, null);
        entityManager.flush();

        List<Dish> available = dishRepository.findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId(), DishStatus.AVAILABLE);
        assertThat(available).hasSize(1);
        assertThat(available.get(0).getName()).isEqualTo("Available Dish");

        List<Dish> onHold = dishRepository.findAllByRestaurantIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(restaurant.getId(), DishStatus.ON_HOLD);
        assertThat(onHold).hasSize(1);
        assertThat(onHold.get(0).getName()).isEqualTo("On Hold Dish");
    }
}
