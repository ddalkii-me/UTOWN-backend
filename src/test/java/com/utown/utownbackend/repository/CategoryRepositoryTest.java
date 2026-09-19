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
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        User owner = new User();
        owner.setEmail("cat_owner@test.com");
        owner.setPhone("01055555555");
        owner.setName("Category Owner");
        owner.setPassword("secret");
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setStatus(UserStatus.ACTIVE);
        entityManager.persist(owner);

        City city = new City();
        city.setName("Seoul");
        entityManager.persist(city);

        RestaurantType type = new RestaurantType();
        type.setName("Burger");
        entityManager.persist(type);

        restaurant = new Restaurant();
        restaurant.setName("Burger Joint");
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(restaurant);

        entityManager.flush();
    }

    private Category createCategory(String name, Integer priority, LocalDateTime deletedAt) {
        Category category = new Category();
        category.setName(name);
        category.setRestaurant(restaurant);
        category.setPriority(priority);
        category.setDeletedAt(deletedAt);
        return entityManager.persist(category);
    }

    @Test
    @DisplayName("findAllByDeletedAtIsNull - returns active categories, excluding soft-deleted")
    void findAllByDeletedAtIsNull_success() {
        createCategory("Burgers", 1, null);
        createCategory("Sides", 2, null);
        createCategory("Deleted Drinks", 3, LocalDateTime.now());
        entityManager.flush();

        List<Category> result = categoryRepository.findAllByDeletedAtIsNull();

        assertThat(result).hasSize(2)
                .extracting(Category::getName)
                .containsExactlyInAnyOrder("Burgers", "Sides");
    }

    @Test
    @DisplayName("findByIdAndDeletedAtIsNull - returns category when active, empty if deleted")
    void findByIdAndDeletedAtIsNull_success() {
        Category active = createCategory("Desserts", 1, null);
        Category deleted = createCategory("Old Shakes", 2, LocalDateTime.now());
        entityManager.flush();

        Optional<Category> foundActive = categoryRepository.findByIdAndDeletedAtIsNull(active.getId());
        Optional<Category> foundDeleted = categoryRepository.findByIdAndDeletedAtIsNull(deleted.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Desserts");
        assertThat(foundDeleted).isEmpty();
    }

    @Test
    @DisplayName("findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc - returns ordered categories for specific restaurant, excluding soft-deleted and other restaurants")
    void findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc_success() {
        // Categories for restaurant
        createCategory("Drinks", 3, null);
        createCategory("Appetizers", 1, null);
        createCategory("Mains", 2, null);
        createCategory("Old Category", 0, LocalDateTime.now());

        // Category for another restaurant
        Restaurant otherRestaurant = new Restaurant();
        otherRestaurant.setName("Other Place");
        otherRestaurant.setCity(restaurant.getCity());
        otherRestaurant.setType(restaurant.getType());
        otherRestaurant.setOwner(restaurant.getOwner());
        otherRestaurant.setStatus(RestaurantStatus.OPEN);
        entityManager.persist(otherRestaurant);

        Category otherCat = new Category();
        otherCat.setName("Other Food");
        otherCat.setRestaurant(otherRestaurant);
        otherCat.setPriority(1);
        entityManager.persist(otherCat);
        entityManager.flush();

        List<Category> result = categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurant.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Appetizers");
        assertThat(result.get(1).getName()).isEqualTo("Mains");
        assertThat(result.get(2).getName()).isEqualTo("Drinks");
    }
}
