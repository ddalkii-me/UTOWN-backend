package com.utown.utownbackend.util;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class TestDataFactory {

    // ── Entity Factory Methods ────────────────────────────────────────

    public static User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setPhone("010" + String.format("%08d", id != null ? id : 0));
        user.setName("Test User " + id);
        user.setPassword("hashedpassword" + id);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static City createCity(Long id, String name) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setCreatedAt(LocalDateTime.now());
        city.setUpdatedAt(LocalDateTime.now());
        return city;
    }

    public static DeliveryArea createDeliveryArea(Long id, String name, City city) {
        DeliveryArea deliveryArea = new DeliveryArea();
        deliveryArea.setId(id);
        deliveryArea.setName(name);
        deliveryArea.setCity(city);
        deliveryArea.setCreatedAt(LocalDateTime.now());
        deliveryArea.setUpdatedAt(LocalDateTime.now());
        return deliveryArea;
    }

    public static Address createAddress(Long id, User user, City city, DeliveryArea deliveryArea) {
        Address address = new Address();
        address.setId(id);
        address.setUser(user);
        address.setCity(city);
        address.setDeliveryArea(deliveryArea);
        address.setLabel("Label " + id);
        address.setRecipientName("Recipient " + id);
        address.setPhone("010-1234-5678");
        address.setAddressLine("Street " + id);
        address.setPostalCode("12345");
        address.setLatitude(new BigDecimal("37.4979"));
        address.setLongitude(new BigDecimal("127.0276"));
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());
        return address;
    }

    public static RestaurantType createRestaurantType(Long id, String name) {
        RestaurantType type = new RestaurantType();
        type.setId(id);
        type.setName(name);
        type.setDescription("Description for " + name);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());
        return type;
    }

    public static Restaurant createRestaurant(Long id, String name, City city, RestaurantType type, User owner) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(id);
        restaurant.setName(name);
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setOwner(owner);
        restaurant.setDescription("Description " + id);
        restaurant.setAddress("Address " + id);
        restaurant.setPhone("02-123-4567");
        restaurant.setLogoUrl("http://example.com/logo" + id + ".png");
        restaurant.setMinimumOrderAmount(new BigDecimal("10000"));
        restaurant.setLatitude(new BigDecimal("37.4979"));
        restaurant.setLongitude(new BigDecimal("127.0276"));
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant.setCreatedAt(LocalDateTime.now());
        restaurant.setUpdatedAt(LocalDateTime.now());
        return restaurant;
    }

    public static Category createCategory(Long id, String name, Restaurant restaurant) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setRestaurant(restaurant);
        category.setDescription("Category Description " + id);
        category.setImageUrl("http://example.com/cat" + id + ".png");
        category.setPriority(id.intValue());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    public static Dish createDish(Long id, String name, Restaurant restaurant, Category category) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setName(name);
        dish.setRestaurant(restaurant);
        dish.setCategory(category);
        dish.setDescription("Dish Description " + id);
        dish.setPrice(new BigDecimal("10000"));
        dish.setImageUrl("http://example.com/image" + id + ".jpg");
        dish.setStatus(DishStatus.AVAILABLE);
        dish.setSortOrder(id.intValue());
        dish.setCreatedAt(LocalDateTime.now());
        dish.setUpdatedAt(LocalDateTime.now());
        return dish;
    }

    public static DishOptionGroup createDishOptionGroup(Long id, String name, Dish dish) {
        DishOptionGroup group = new DishOptionGroup();
        group.setId(id);
        group.setName(name);
        group.setDish(dish);
        group.setRequired(true);
        group.setMinSelections(1);
        group.setMaxSelections(1);
        group.setSortOrder(id.intValue());
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        return group;
    }

    public static DishOption createDishOption(Long id, String name, DishOptionGroup group) {
        DishOption option = new DishOption();
        option.setId(id);
        option.setName(name);
        option.setOptionGroup(group);
        option.setAdditionalPrice(new BigDecimal("1000"));
        option.setSortOrder(id.intValue());
        option.setStatus(DishOptionStatus.AVAILABLE);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        return option;
    }

    // ── DTO Factory Methods ───────────────────────────────────────────

    public static CityRequestDto createCityRequestDto(String name) {
        return new CityRequestDto(name);
    }

    public static CityResponseDto createCityResponseDto(Long id, String name) {
        return new CityResponseDto(id, name);
    }

    public static RestaurantTypeRequestDto createRestaurantTypeRequestDto(String name) {
        return new RestaurantTypeRequestDto(name, "Description for " + name);
    }

    public static RestaurantTypeResponseDto createRestaurantTypeResponseDto(Long id, String name) {
        return new RestaurantTypeResponseDto(id, name, "Description for " + name);
    }

    public static DeliveryAreaRequestDto createDeliveryAreaRequestDto(Long cityId, String name) {
        return new DeliveryAreaRequestDto(cityId, name);
    }

    public static DeliveryAreaResponseDto createDeliveryAreaResponseDto(Long id, Long cityId, String name) {
        return new DeliveryAreaResponseDto(id, cityId, name);
    }

    public static CategoryRequestDto createCategoryRequestDto(Long restaurantId, String name) {
        return new CategoryRequestDto(restaurantId, name, "Category description", "http://example.com/cat.png", 1);
    }

    public static CategoryResponseDto createCategoryResponseDto(Long id, Long restaurantId, String name) {
        return new CategoryResponseDto(id, restaurantId, name, "Category description", "http://example.com/cat.png", 1);
    }

    public static AddressRequestDto createAddressRequestDto(Long userId, Long cityId, Long deliveryAreaId) {
        return new AddressRequestDto(
                userId, cityId, deliveryAreaId,
                "Home", "Test Recipient", "010-1234-5678",
                "123 Test Street", "12345",
                new BigDecimal("37.4979"), new BigDecimal("127.0276")
        );
    }

    public static AddressResponseDto createAddressResponseDto(Long id, Long userId, Long cityId, Long deliveryAreaId) {
        return new AddressResponseDto(
                id, userId, cityId, deliveryAreaId,
                "Home", "Test Recipient", "010-1234-5678",
                "123 Test Street", "12345",
                new BigDecimal("37.4979"), new BigDecimal("127.0276")
        );
    }

    public static RestaurantRequestDto createRestaurantRequestDto(Long ownerId, Long typeId, Long cityId, String name) {
        return new RestaurantRequestDto(
                ownerId, typeId, cityId, name,
                "Restaurant Description", "Restaurant Address 123", "02-123-4567",
                "http://example.com/logo.png",
                new BigDecimal("37.4979"), new BigDecimal("127.0276"),
                new BigDecimal("10000"), RestaurantStatus.OPEN
        );
    }

    public static RestaurantResponseDto createRestaurantResponseDto(Long id, Long ownerId, Long typeId, Long cityId, String name) {
        return new RestaurantResponseDto(
                id, ownerId, typeId, cityId, name,
                "Restaurant Description", "Restaurant Address 123", "02-123-4567",
                "http://example.com/logo.png",
                new BigDecimal("37.4979"), new BigDecimal("127.0276"),
                new BigDecimal("10000"), RestaurantStatus.OPEN
        );
    }

    public static WorkingHoursDto createWorkingHoursDto(DayOfWeek dayOfWeek) {
        return new WorkingHoursDto(dayOfWeek, LocalTime.of(9, 0), LocalTime.of(22, 0), false);
    }

    public static DishRequestDto createDishRequestDto(Long restaurantId, Long categoryId, String name) {
        return new DishRequestDto(
                restaurantId, categoryId, name,
                new BigDecimal("12000"), "Delicious dish description",
                "http://example.com/dish.jpg", DishStatus.AVAILABLE, 1
        );
    }

    public static DishResponseDto createDishResponseDto(Long id, Long restaurantId, Long categoryId, String name) {
        return new DishResponseDto(
                id, restaurantId, categoryId, name,
                new BigDecimal("12000"), "Delicious dish description",
                "http://example.com/dish.jpg", DishStatus.AVAILABLE, 1, null
        );
    }

    public static DishOptionGroupRequestDto createDishOptionGroupRequestDto(Long dishId, String name) {
        return new DishOptionGroupRequestDto(dishId, name, true, 1, 1, 1);
    }

    public static DishOptionGroupResponseDto createDishOptionGroupResponseDto(Long id, Long dishId, String name) {
        return new DishOptionGroupResponseDto(id, dishId, name, true, 1, 1, 1, null);
    }

    public static DishOptionRequestDto createDishOptionRequestDto(Long optionGroupId, String name) {
        return new DishOptionRequestDto(name, optionGroupId, new BigDecimal("1500"), 1, DishOptionStatus.AVAILABLE);
    }

    public static DishOptionResponseDto createDishOptionResponseDto(Long id, Long optionGroupId, String name) {
        return new DishOptionResponseDto(id, optionGroupId, name, new BigDecimal("1500"), 1, DishOptionStatus.AVAILABLE, null);
    }

    public static AuthCode createAuthCode(Long id, User user, String codeHash, AuthCodePurpose purpose) {
        AuthCode authCode = new AuthCode();
        authCode.setId(id);
        authCode.setUser(user);
        authCode.setCodeHash(codeHash);
        authCode.setPurpose(purpose);
        authCode.setAttempts(0);
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        authCode.setCreatedAt(LocalDateTime.now());
        authCode.setUpdatedAt(LocalDateTime.now());
        return authCode;
    }

    public static RiderProfile createRiderProfile(Long id, User user, TransportType transportType, Boolean availability, RiderStatus status) {
        RiderProfile rider = new RiderProfile();
        rider.setId(id);
        rider.setUser(user);
        rider.setTransportType(transportType != null ? transportType : TransportType.MOTORCYCLE);
        rider.setAvailability(availability != null ? availability : true);
        rider.setStatus(status != null ? status : RiderStatus.ACTIVE);
        rider.setCreatedAt(LocalDateTime.now());
        rider.setUpdatedAt(LocalDateTime.now());
        return rider;
    }

    public static RiderProfileRequestDto createRiderProfileRequestDto(Long userId, TransportType transportType) {
        return new RiderProfileRequestDto(
                userId,
                transportType != null ? transportType : TransportType.MOTORCYCLE,
                true,
                RiderStatus.ACTIVE
        );
    }

    public static RiderProfileResponseDto createRiderProfileResponseDto(Long id, Long userId) {
        return new RiderProfileResponseDto(
                id,
                userId,
                "Test Rider",
                "01012345678",
                "rider@example.com",
                TransportType.MOTORCYCLE,
                true,
                RiderStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}

