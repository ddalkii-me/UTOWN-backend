package com.utown.utownbackend.seeder;

import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RiderProfileRepository riderProfileRepository;
    private final CityRepository cityRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;
    private final RestaurantTypeRepository restaurantTypeRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantWorkingHoursRepository workingHoursRepository;
    private final CategoryRepository categoryRepository;
    private final DishRepository dishRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final RatingRepository ratingRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final DishOptionRepository dishOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting demo data seeder...");

        log.info("Seeding users...");
        User admin = findOrCreateUser("Admin", "User", "admin@demo.com", UserRole.ADMIN, "01011111111");
        User owner1 = findOrCreateUser("Restaurant", "Owner 1", "owner1@demo.com", UserRole.RESTAURANT_OWNER, "01022222222");
        User owner2 = findOrCreateUser("Restaurant", "Owner 2", "owner2@demo.com", UserRole.RESTAURANT_OWNER, "01022222223");
        User customer = findOrCreateUser("Hungry", "Customer", "customer@demo.com", UserRole.CUSTOMER, "01033333333");
        User rider = findOrCreateUser("Speedy", "Rider", "rider@demo.com", UserRole.RIDER, "01044444444");

        if (riderProfileRepository.findAll().stream().noneMatch(rp -> rp.getUser().getId().equals(rider.getId()))) {
            RiderProfile riderProfile = new RiderProfile();
            riderProfile.setUser(rider);
            riderProfile.setTransportType(TransportType.MOTORCYCLE);
            riderProfile.setAvailability(true);
            riderProfile.setStatus(RiderStatus.ACTIVE);
            riderProfileRepository.save(riderProfile);
        }

        log.info("Seeding geography...");
        City seoul = findOrCreateCity("Seoul");
        DeliveryArea gangnam = findOrCreateArea(seoul, "Gangnam");

        log.info("Seeding restaurant data...");
        RestaurantType typePizza = findOrCreateRestaurantType("Pizza");
        RestaurantType typeBurger = findOrCreateRestaurantType("Burger");

        Restaurant rest1 = findOrCreateRestaurant("Demo Pizza Place", owner1, typePizza, seoul, "123 Pizza Street", "02-123-4567", "37.4979", "127.0276", "15000");
        Restaurant rest2 = findOrCreateRestaurant("Demo Burger House", owner2, typeBurger, seoul, "456 Burger Ave", "02-987-6543", "37.5110", "127.0590", "12000");

        findOrCreateDeliveryAreaMapping(rest1, gangnam);
        findOrCreateDeliveryAreaMapping(rest2, gangnam);

        Category cat1 = findOrCreateCategory(rest1, "Main Course");
        Category cat2 = findOrCreateCategory(rest2, "Burgers");

        Dish dish1 = findOrCreateDish(rest1, cat1, "Pepperoni Pizza", "18000", "https://example.com/pizza.jpg");
        Dish dish2 = findOrCreateDish(rest2, cat2, "Cheeseburger", "8000", "https://example.com/burger.jpg");

        DishOptionGroup optionGroup = findOrCreateOptionGroup(dish1, "Size");
        findOrCreateOption(optionGroup, "Large", "3000");

        log.info("Seeding historical orders and ratings...");
        Address address = findOrCreateAddress(customer, seoul, gangnam, "Customer Home 101");

        createOrderIfNotExists(customer, rest1, address, dish1, 5, "Amazing pizza!");
        createOrderIfNotExists(customer, rest2, address, dish2, 4, "Great burger.");

        log.info("Demo data seeding completed!");
    }

    private User findOrCreateUser(String firstName, String lastName, String email, UserRole role, String phone) {
        return userRepository.findByEmailAndDeletedAtIsNull(email).orElseGet(() -> {
            User user = new User();
            user.setName(firstName + " " + lastName);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(role);
            user.setPhone(com.utown.utownbackend.util.PhoneUtil.normalizePhone(phone));
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        });
    }

    private City findOrCreateCity(String name) {
        return cityRepository.findAll().stream().filter(c -> c.getName().equals(name)).findFirst().orElseGet(() -> {
            City city = new City();
            city.setName(name);
            return cityRepository.save(city);
        });
    }

    private DeliveryArea findOrCreateArea(City city, String name) {
        return deliveryAreaRepository.findAll().stream().filter(a -> a.getName().equals(name) && a.getCity().getId().equals(city.getId())).findFirst().orElseGet(() -> {
            DeliveryArea area = new DeliveryArea();
            area.setCity(city);
            area.setName(name);
            return deliveryAreaRepository.save(area);
        });
    }

    private RestaurantType findOrCreateRestaurantType(String name) {
        return restaurantTypeRepository.findAll().stream().filter(t -> t.getName().equals(name)).findFirst().orElseGet(() -> {
            RestaurantType type = new RestaurantType();
            type.setName(name);
            return restaurantTypeRepository.save(type);
        });
    }

    private Restaurant findOrCreateRestaurant(String name, User owner, RestaurantType type, City city, String address, String phone, String lat, String lng, String minOrder) {
        Optional<Restaurant> optionalRest = restaurantRepository.findAll().stream().filter(r -> r.getName().equals(name)).findFirst();
        if (optionalRest.isPresent()) {
            return optionalRest.get();
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setAddress(address);
        restaurant.setDescription("Best demo " + type.getName().toLowerCase() + " in town!");
        restaurant.setPhone(phone);
        restaurant.setLatitude(new BigDecimal(lat));
        restaurant.setLongitude(new BigDecimal(lng));
        restaurant.setMinimumOrderAmount(new BigDecimal(minOrder));
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant = restaurantRepository.save(restaurant);

        for (DayOfWeek day : DayOfWeek.values()) {
            RestaurantWorkingHours hours = new RestaurantWorkingHours();
            hours.setRestaurant(restaurant);
            hours.setDayOfWeek(day);
            hours.setOpenTime(LocalTime.of(10, 0));
            hours.setCloseTime(LocalTime.of(23, 0));
            hours.setDayOff(false);
            workingHoursRepository.save(hours);
        }
        return restaurant;
    }

    private Category findOrCreateCategory(Restaurant restaurant, String name) {
        return categoryRepository.findAll().stream().filter(c -> c.getName().equals(name) && c.getRestaurant().getId().equals(restaurant.getId())).findFirst().orElseGet(() -> {
            Category category = new Category();
            category.setRestaurant(restaurant);
            category.setName(name);
            category.setPriority(1);
            return categoryRepository.save(category);
        });
    }

    private Dish findOrCreateDish(Restaurant restaurant, Category category, String name, String price, String img) {
        return dishRepository.findAll().stream().filter(d -> d.getCategory().getId().equals(category.getId())).filter(d -> d.getName().equals(name)).findFirst().orElseGet(() -> {
                    Dish dish = new Dish();
                    dish.setCategory(category);
                    dish.setName(name);
                    dish.setDescription("Classic " + name);
                    dish.setPrice(new BigDecimal(price));
                    dish.setImageUrl(img);
                    dish.setRestaurant(restaurant);
                    dish.setStatus(DishStatus.AVAILABLE);
                    return dishRepository.save(dish);
                });
    }

    private DishOptionGroup findOrCreateOptionGroup(Dish dish, String name) {
        return dishOptionGroupRepository.findAll().stream().filter(g -> g.getDish().getId().equals(dish.getId())).filter(g -> g.getName().equals(name)).findFirst().orElseGet(() -> {
                    DishOptionGroup optionGroup = new DishOptionGroup();
                    optionGroup.setDish(dish);
                    optionGroup.setName(name);
                    optionGroup.setRequired(true);
                    return dishOptionGroupRepository.save(optionGroup);
                });
    }

    private DishOption findOrCreateOption(DishOptionGroup group, String name, String price) {
        return dishOptionRepository.findAll().stream().filter(o -> o.getOptionGroup().getId().equals(group.getId())).filter(o -> o.getName().equals(name)).findFirst().orElseGet(() -> {
                    DishOption option = new DishOption();
                    option.setOptionGroup(group);
                    option.setName(name);
                    option.setAdditionalPrice(new BigDecimal(price));
                    option.setStatus(DishOptionStatus.AVAILABLE);
                    return dishOptionRepository.save(option);
                });
    }

    private Address findOrCreateAddress(User user, City city, DeliveryArea area, String line) {
        return addressRepository.findAll().stream().filter(a -> a.getUser().getId().equals(user.getId())).filter(a -> a.getAddressLine().equals(line)).findFirst().orElseGet(() -> {
                    Address address = new Address();
                    address.setUser(user);
                    address.setCity(city);
                    address.setDeliveryArea(area);
                    address.setAddressLine(line);
                    address.setRecipientName(user.getName());
                    address.setPhone(user.getPhone());
                    return addressRepository.save(address);
                });
    }

    private void createOrderIfNotExists(User user, Restaurant restaurant, Address address, Dish dish, int ratingScore, String comment) {
        List<Order> userOrders = orderRepository.findAll().stream().filter(o -> o.getUser().getId().equals(user.getId())).toList();
        if (userOrders.stream().anyMatch(o -> o.getRestaurant().getId().equals(restaurant.getId()))) {
            return; // Order already exists for this restaurant
        }

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setAddress(address);
        order.setStatus(OrderStatus.DELIVERED);
        order.setSubtotal(dish.getPrice());
        order.setDeliveryFee(new BigDecimal("3000"));
        order.setTotalAmount(dish.getPrice().add(new BigDecimal("3000")));
        order.setCurrency("KRW");
        order.setPaymentMethod(PaymentMethod.CARD);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setCreatedAt(LocalDateTime.now().minusDays(2));
        order = orderRepository.save(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setDish(dish);
        orderItem.setDishName(dish.getName());
        orderItem.setUnitPrice(dish.getPrice());
        orderItem.setQuantity(1);
        orderItem.setSubtotal(dish.getPrice());
        orderItemRepository.save(orderItem);

        Rating rating = new Rating();
        rating.setOrder(order);
        rating.setUser(user);
        rating.setRestaurant(restaurant);
        rating.setScore(ratingScore);
        rating.setComment(comment);
        ratingRepository.save(rating);

        Double avg = ratingRepository.getAverageScoreByRestaurantId(restaurant.getId());
        if (avg != null) {
            restaurant.setAverageRating(BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
            restaurantRepository.save(restaurant);
        }
    }

    private void findOrCreateDeliveryAreaMapping(Restaurant restaurant, DeliveryArea area) {
        if (!restaurantDeliveryAreaRepository.existsByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(restaurant.getId(), area.getId())) {
            RestaurantDeliveryArea rda = new RestaurantDeliveryArea();
            rda.setRestaurant(restaurant);
            rda.setDeliveryArea(area);
            restaurantDeliveryAreaRepository.save(rda);
        }
    }
}
