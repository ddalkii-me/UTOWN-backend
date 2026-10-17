package com.utown.utownbackend.integration;

import tools.jackson.databind.ObjectMapper;
import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import com.utown.utownbackend.security.JwtUtil;
import com.utown.utownbackend.security.CustomUserDetails;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderJourneyIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private RestaurantTypeRepository restaurantTypeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DishRepository dishRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private DeliveryAreaRepository deliveryAreaRepository;

    @Autowired
    private RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RiderProfileRepository riderProfileRepository;

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Shared state between tests
    private static boolean seeded = false;
    private static String customerToken;
    private static String ownerToken;
    private static String riderToken;
    private static Long restaurantId;
    private static Long dishId;
    private static Long customerAddressId;
    private static Long currentOrderId;
    private static Long currentDeliveryAssignmentId;
    private static Long riderId;

    @BeforeEach
    public void setup() {
        if (seeded) return;

        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Create City and Delivery Area
        City city = new City();
        city.setName("Seoul Journey " + runId);
        city = cityRepository.save(city);

        DeliveryArea area = new DeliveryArea();
        area.setName("Gangnam " + runId);
        area.setCity(city);
        area = deliveryAreaRepository.save(area);

        // Create Users
        User customer = new User();
        customer.setEmail("customer.journey." + runId + "@test.com");
        customer.setPhone("01011112222");
        customer.setPassword(passwordEncoder.encode("password123"));
        customer.setRole(UserRole.CUSTOMER);
        customer.setName("Customer User");
        customer.setStatus(UserStatus.ACTIVE);
        customer = userRepository.save(customer);

        User owner = new User();
        owner.setEmail("owner.journey." + runId + "@test.com");
        owner.setPhone("01033334444");
        owner.setPassword(passwordEncoder.encode("password123"));
        owner.setRole(UserRole.RESTAURANT_OWNER);
        owner.setName("Owner User");
        owner.setStatus(UserStatus.ACTIVE);
        owner = userRepository.save(owner);

        User rider = new User();
        rider.setEmail("rider.journey." + runId + "@test.com");
        rider.setPhone("01055556666");
        rider.setPassword(passwordEncoder.encode("password123"));
        rider.setRole(UserRole.RIDER);
        rider.setName("Rider User");
        rider.setStatus(UserStatus.ACTIVE);
        rider = userRepository.save(rider);

        RiderProfile riderProfile = new RiderProfile();
        riderProfile.setUser(rider);
        riderProfile.setTransportType(TransportType.MOTORCYCLE);
        riderProfile.setAvailability(true);
        riderProfile.setStatus(RiderStatus.ACTIVE);
        riderProfile = riderProfileRepository.save(riderProfile);
        riderId = riderProfile.getId();

        // Generate Tokens
        customerToken = jwtUtil.generateToken(new CustomUserDetails(customer));
        ownerToken = jwtUtil.generateToken(new CustomUserDetails(owner));
        riderToken = jwtUtil.generateToken(new CustomUserDetails(rider));

        // Create Restaurant
        RestaurantType type = new RestaurantType();
        type.setName("Fast Food " + runId);
        type = restaurantTypeRepository.save(type);

        Restaurant restaurant = new Restaurant();
        restaurant.setOwner(owner);
        restaurant.setCity(city);
        restaurant.setType(type);
        restaurant.setName("Test Burger " + runId);
        restaurant.setAddress("Burger St");
        restaurant.setPhone("02-999-9999");
        restaurant.setMinimumOrderAmount(new BigDecimal("10000"));
        restaurant.setStatus(RestaurantStatus.OPEN);
        restaurant = restaurantRepository.save(restaurant);
        restaurantId = restaurant.getId();

        // Create Restaurant Delivery Area
        RestaurantDeliveryArea rda = new RestaurantDeliveryArea();
        rda.setRestaurant(restaurant);
        rda.setDeliveryArea(area);
        restaurantDeliveryAreaRepository.save(rda);

        // Create Menu
        Category category = new Category();
        category.setRestaurant(restaurant);
        category.setName("Burgers");
        category = categoryRepository.save(category);

        Dish dish = new Dish();
        dish.setCategory(category);
        dish.setRestaurant(restaurant);
        dish.setName("Cheeseburger");
        dish.setPrice(new BigDecimal("8000"));
        dish.setStatus(DishStatus.AVAILABLE);
        dish = dishRepository.save(dish);
        dishId = dish.getId();

        // Create Customer Address
        Address address = new Address();
        address.setUser(customer);
        address.setCity(city);
        address.setDeliveryArea(area);
        address.setAddressLine("Customer Home 123");
        address.setRecipientName("Customer");
        address.setPhone("01011112222");
        address = addressRepository.save(address);
        customerAddressId = address.getId();

        seeded = true;
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Customer places an order successfully")
    public void testCustomerPlacesOrder() throws Exception {
        OrderRequestDto createDto = new OrderRequestDto(
                null,
                restaurantId,
                customerAddressId,
                "Please leave at door",
                PaymentMethod.CARD,
                List.of(new OrderItemRequestDto(dishId, 2, null))
        );

        MvcResult result = mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.name()))
                .andReturn();

        OrderResponseDto response = objectMapper.readValue(result.getResponse().getContentAsString(), OrderResponseDto.class);
        currentOrderId = response.id();
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Owner accepts the order")
    public void testOwnerAcceptsOrder() throws Exception {
        OrderAcceptRequestDto updateDto = new OrderAcceptRequestDto(15);

        mockMvc.perform(patch("/api/orders/" + currentOrderId + "/accept")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.ACCEPTED.name()));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Customer tries to accept order and gets forbidden")
    public void testCustomerCannotAcceptOrder() throws Exception {
        OrderAcceptRequestDto updateDto = new OrderAcceptRequestDto(20);

        mockMvc.perform(patch("/api/orders/" + currentOrderId + "/accept")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Owner prepares and readies the order")
    public void testOwnerPreparesAndReadiesOrder() throws Exception {
        mockMvc.perform(patch("/api/orders/" + currentOrderId + "/prepare")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.IN_PREPARATION.name()));

        mockMvc.perform(patch("/api/orders/" + currentOrderId + "/ready")
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.READY_FOR_PICKUP.name()));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Owner creates delivery assignment")
    public void testOwnerCreatesDeliveryAssignment() throws Exception {
        DeliveryAssignmentRequestDto requestDto = new DeliveryAssignmentRequestDto(currentOrderId, riderId);

        MvcResult result = mockMvc.perform(post("/api/delivery-assignments")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(DeliveryAssignmentStatus.ASSIGNED.name()))
                .andReturn();

        DeliveryAssignmentResponseDto response = objectMapper.readValue(result.getResponse().getContentAsString(), DeliveryAssignmentResponseDto.class);
        currentDeliveryAssignmentId = response.id();
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Rider accepts, picks up, and delivers")
    public void testRiderDeliveryJourney() throws Exception {
        mockMvc.perform(patch("/api/delivery-assignments/" + currentDeliveryAssignmentId + "/accept")
                .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeliveryAssignmentStatus.ACCEPTED.name()));

        mockMvc.perform(patch("/api/delivery-assignments/" + currentDeliveryAssignmentId + "/pickup")
                .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeliveryAssignmentStatus.PICKED_UP.name()));

        mockMvc.perform(patch("/api/delivery-assignments/" + currentDeliveryAssignmentId + "/deliver")
                .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeliveryAssignmentStatus.DELIVERED.name()));
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Customer completes the order and leaves rating")
    public void testCustomerCompletesAndRates() throws Exception {
        mockMvc.perform(patch("/api/orders/" + currentOrderId + "/complete")
                .header("Authorization", "Bearer " + ownerToken)) // Owner marks completed
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(OrderStatus.COMPLETED.name()));

        RatingRequestDto ratingDto = new RatingRequestDto(5, "Fast and tasty!");

        mockMvc.perform(post("/api/orders/" + currentOrderId + "/ratings")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ratingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.comment").value("Fast and tasty!"));
    }
}
