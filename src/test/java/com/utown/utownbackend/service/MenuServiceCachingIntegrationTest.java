package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantMenuResponseDto;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantStatus;
import com.utown.utownbackend.repository.CategoryRepository;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import com.utown.utownbackend.repository.DishRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.utown.utownbackend.dto.CategoryRequestDto;
import com.utown.utownbackend.entity.Category;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@SpringBootTest(properties = "spring.cache.type=redis")
@Testcontainers(disabledWithoutDocker = true)
class MenuServiceCachingIntegrationTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MenuService menuService;

    @MockitoBean
    private RestaurantRepository restaurantRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private DishRepository dishRepository;

    @MockitoBean
    private DishOptionGroupRepository dishOptionGroupRepository;

    @MockitoBean
    private DishOptionRepository dishOptionRepository;

    @Autowired
    private DishService dishService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CategoryService categoryService;

    @Test
    void getRestaurantMenu_sameRestaurantUsesCachedValue() throws InterruptedException {
        Long restaurantId = 10L;

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Place");
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(5));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId))
                .thenReturn(List.of());
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId))
                .thenReturn(List.of());

        var cache = cacheManager.getCache("restaurantMenus");
        assertNotNull(cache, "restaurantMenus cache should exist");
        cache.evict(restaurantId);


        var firstResponse = menuService.getRestaurantMenu(restaurantId);

        waitForCacheEntry(cache, restaurantId);

        var secondResponse = menuService.getRestaurantMenu(restaurantId);

        assertEquals(firstResponse, secondResponse);

        verify(restaurantRepository, times(1))
                .findByIdAndDeletedAtIsNull(restaurantId);
        verify(categoryRepository, times(1))
                .findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId);
        verify(dishRepository, times(1))
                .findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId);
    }

    @Test
    void deletingDishEvictsCachedRestaurantMenu(){
        Long restaurantId = 20L;
        Long dishId = 200L;

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Place");
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(5));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId))
                .thenReturn(List.of());
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId))
                .thenReturn(List.of());

        var menuCache = cacheManager.getCache("restaurantMenus");
        assertNotNull(menuCache, "restaurantMenus cache should exist");
        menuCache.evict(restaurantId);

        // Load and cache the menu.
        menuService.getRestaurantMenu(restaurantId);

        Dish dish = new Dish();
        dish.setId(dishId);

        dish.setRestaurant(restaurant);

        when(dishOptionGroupRepository.existsByDishIdAndDeletedAtIsNull(dishId))
                .thenReturn(false);
        when(dishRepository.findByIdAndDeletedAtIsNull(dishId))
                .thenReturn(Optional.of(dish));

        dishService.deleteDish(dishId);

        verify(dishRepository).findByIdAndDeletedAtIsNull(dishId);

        assertNull(menuCache.get(restaurantId),
                "Deleting a dish should evict the cached menu");

        // The menu should reload after eviction.
        menuService.getRestaurantMenu(restaurantId);

        verify(restaurantRepository, times(2))
                .findByIdAndDeletedAtIsNull(restaurantId);
    }

    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    @Test
    void updatingCategoryEvictsCachedRestaurantMenu() {
        Long restaurantId = 30L;
        Long categoryId = 300L;

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Place");
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(5));

        Category category = new Category();
        category.setId(categoryId);
        category.setRestaurant(restaurant);
        category.setName("Old category");

        when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(categoryRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId))
                .thenReturn(List.of(), List.of(category));
        when(dishRepository.findAllByRestaurantIdAndDeletedAtIsNullOrderBySortOrderAsc(restaurantId))
                .thenReturn(List.of());

        when(categoryRepository.findById(categoryId))
                .thenReturn(Optional.of(category));
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var menuCache = cacheManager.getCache("restaurantMenus");
        assertNotNull(menuCache, "restaurantMenus cache should exist");
        menuCache.evict(restaurantId);

        // The first menu request receives no categories and is cached.
        var firstResponse = menuService.getRestaurantMenu(restaurantId);
        assertTrue(firstResponse.categories().isEmpty());

        CategoryRequestDto request = new CategoryRequestDto(
                restaurantId,
                "Updated category",
                "Updated description",
                null,
                1
        );

        categoryService.updateCategory(categoryId, request);

        verify(categoryRepository).findById(categoryId);

        var refreshedResponse = menuService.getRestaurantMenu(restaurantId);

        verify(categoryRepository, times(2))
                .findAllByRestaurantIdAndDeletedAtIsNullOrderByPriorityAsc(restaurantId);

        assertEquals(1, refreshedResponse.categories().size());
        assertEquals("Updated category", refreshedResponse.categories().get(0).name());

        verify(restaurantRepository, times(2))
                .findByIdAndDeletedAtIsNull(restaurantId);
    }

    private void waitForCacheEntry(Cache cache, Long restaurantId)
            throws InterruptedException {

        for (int attempt = 0; attempt < 50; attempt++) {
            if (cache.get(restaurantId) != null) {
                return;
            }

            Thread.sleep(100);
        }

        fail("Redis cache entry was not created for restaurant " + restaurantId);
    }
}