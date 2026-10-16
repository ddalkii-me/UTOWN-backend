package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantOwnerUpdateRequestDto;
import com.utown.utownbackend.dto.RestaurantRequestDto;
import com.utown.utownbackend.dto.RestaurantResponseDto;
import com.utown.utownbackend.dto.WorkingHoursDto;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utown.utownbackend.util.TestDataFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantTypeRepository restaurantTypeRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RestaurantWorkingHoursRepository workingHoursRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache restaurantMenusCache;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private User owner;
    private RestaurantType type;
    private City city;
    private Restaurant restaurant;
    private RestaurantRequestDto requestDto;
    private RestaurantOwnerUpdateRequestDto ownerUpdateRequestDto;


    @BeforeEach
    void setUp() {
        owner = TestDataFactory.createUser(1L);
        type = TestDataFactory.createRestaurantType(1L, "Test Type");
        city = TestDataFactory.createCity(1L, "Test City");
        restaurant = TestDataFactory.createRestaurant(1L, "Test Restaurant", city, type, owner);

        restaurant.setDescription("Test description");
        restaurant.setAddress("Test address");
        restaurant.setPhone("01012345678");
        restaurant.setLogoUrl("logo.png");
        restaurant.setLatitude(BigDecimal.valueOf(37));
        restaurant.setLongitude(BigDecimal.valueOf(127));
        restaurant.setMinimumOrderAmount(BigDecimal.valueOf(10000));
        restaurant.setStatus(RestaurantStatus.OPEN);

        // Used for POST /api/restaurants
        requestDto = new RestaurantRequestDto(
                1L, 1L, 1L,
                "Test Restaurant",
                "Test description",
                "Test address",
                "01012345678",
                "logo.png",
                BigDecimal.valueOf(37),
                BigDecimal.valueOf(127),
                BigDecimal.valueOf(10000),
                RestaurantStatus.OPEN
        );

        // Used for PUT /api/restaurants/{id} by RESTAURANT_OWNER
        // Notice: there is NO ownerId here.
        ownerUpdateRequestDto = new RestaurantOwnerUpdateRequestDto(
                1L,  // typeId
                1L,  // cityId
                "Test Restaurant",
                "Test description",
                "Test address",
                "01012345678",
                "logo.png",
                BigDecimal.valueOf(37),
                BigDecimal.valueOf(127),
                BigDecimal.valueOf(10000),
                RestaurantStatus.OPEN
        );

        lenient().when(cacheManager.getCache("restaurantMenus"))
                .thenReturn(restaurantMenusCache);
    }

    // ── createRestaurant ─────────────────────────────────────────────

    @Test
    @DisplayName("createRestaurant - should create and return restaurant")
    void createRestaurant_shouldReturnSavedRestaurant() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantResponseDto result = restaurantService.createRestaurant(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Restaurant");
        assertThat(result.ownerId()).isEqualTo(1L);
        assertThat(result.typeId()).isEqualTo(1L);
        assertThat(result.cityId()).isEqualTo(1L);
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("createRestaurant - should throw when owner not found")
    void createRestaurant_ownerNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.createRestaurant(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("createRestaurant - should throw when restaurant type not found")
    void createRestaurant_typeNotFound_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.createRestaurant(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("createRestaurant - should throw when city is deleted or not found")
    void createRestaurant_deletedCity_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.createRestaurant(requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    // ── getAllRestaurants ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllRestaurants - should return list of active restaurants with batch-fetched working hours")
    void getAllRestaurants_shouldReturnList() {
        when(restaurantRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(restaurant));
        when(workingHoursRepository.findByRestaurantIdIn(List.of(1L))).thenReturn(List.of());

        List<RestaurantResponseDto> result = restaurantService.getAllRestaurants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Test Restaurant");
        assertThat(result.get(0).isOpenNow()).isTrue();
        verify(restaurantRepository).findAllByDeletedAtIsNull();
        verify(workingHoursRepository).findByRestaurantIdIn(List.of(1L));
        verify(workingHoursRepository, never()).findByRestaurantId(anyLong());
    }

    // ── getRestaurantById ────────────────────────────────────────────

    @Test
    @DisplayName("getRestaurantById - should return restaurant when found")
    void getRestaurantById_shouldReturnRestaurant() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponseDto result = restaurantService.getRestaurantById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Test Restaurant");
    }

    @Test
    @DisplayName("getRestaurantById - should throw when not found")
    void getRestaurantById_shouldThrowWhenNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateRestaurant ─────────────────────────────────────────────

    @Test
    @DisplayName("updateRestaurant - should update and return restaurant")
    void updateRestaurant_shouldUpdateAndReturnRestaurant() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantTypeRepository.findById(1L))
                .thenReturn(Optional.of(type));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(restaurantRepository.save(any(Restaurant.class)))
                .thenReturn(restaurant);

        RestaurantResponseDto result =
                restaurantService.updateRestaurant(1L, ownerUpdateRequestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Test Restaurant");

        verify(restaurantRepository).save(restaurant);
    }


    @Test
    @DisplayName("updateRestaurant - should throw when restaurant not found")
    void updateRestaurant_restaurantNotFound_throwsNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                restaurantService.updateRestaurant(99L, ownerUpdateRequestDto))
                .isInstanceOf(EntityNotFoundException.class);

        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }


    @Test
    @DisplayName("updateRestaurant - should throw when city is deleted")
    void updateRestaurant_deletedCity_throwsNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantTypeRepository.findById(1L))
                .thenReturn(Optional.of(type));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                restaurantService.updateRestaurant(1L, ownerUpdateRequestDto))
                .isInstanceOf(EntityNotFoundException.class);

        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }


    @Test
    @DisplayName("updateRestaurant - should throw when restaurant type not found")
    void updateRestaurant_typeNotFound_throwsNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantTypeRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                restaurantService.updateRestaurant(1L, ownerUpdateRequestDto))
                .isInstanceOf(EntityNotFoundException.class);

        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    // ── deleteRestaurant ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteRestaurant - should soft-delete restaurant")
    void deleteRestaurant_shouldSetDeletedAt() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(1L);

        assertThat(restaurant.getDeletedAt()).isNotNull();
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    @DisplayName("deleteRestaurant - should throw when restaurant not found")
    void deleteRestaurant_shouldThrowWhenNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.deleteRestaurant(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    // ── updateWorkingHourForDay ───────────────────────────────────────

    @Test
    @DisplayName("updateWorkingHourForDay - should create new working hours entry")
    void updateWorkingHourForDay_shouldCreateNewEntry() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(workingHoursRepository.findByRestaurantIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(Optional.empty());

        WorkingHoursDto dto = new WorkingHoursDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0), false);

        restaurantService.updateWorkingHourForDay(1L, DayOfWeek.MONDAY, dto);

        verify(workingHoursRepository).save(any(RestaurantWorkingHours.class));
    }

    @Test
    @DisplayName("updateWorkingHourForDay - should update existing working hours entry")
    void updateWorkingHourForDay_shouldUpdateExistingEntry() {
        RestaurantWorkingHours existingHours = new RestaurantWorkingHours();
        existingHours.setId(1L);
        existingHours.setRestaurant(restaurant);
        existingHours.setDayOfWeek(DayOfWeek.MONDAY);

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(workingHoursRepository.findByRestaurantIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(existingHours));

        WorkingHoursDto dto = new WorkingHoursDto(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(23, 0), false);

        restaurantService.updateWorkingHourForDay(1L, DayOfWeek.MONDAY, dto);

        verify(workingHoursRepository).save(existingHours);
        assertThat(existingHours.getOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(existingHours.getCloseTime()).isEqualTo(LocalTime.of(23, 0));
    }

    @Test
    @DisplayName("updateWorkingHourForDay - should throw when restaurant not found")
    void updateWorkingHourForDay_shouldThrowWhenRestaurantNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        WorkingHoursDto dto = new WorkingHoursDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0), false);

        assertThatThrownBy(() ->
                restaurantService.updateWorkingHourForDay(99L, DayOfWeek.MONDAY, dto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(workingHoursRepository, never()).save(any(RestaurantWorkingHours.class));
    }

    // ── getWorkingHours ──────────────────────────────────────────────

    @Test
    @DisplayName("getWorkingHours - should return list of working hours")
    void getWorkingHours_shouldReturnList() {
        RestaurantWorkingHours hours = new RestaurantWorkingHours();
        hours.setRestaurant(restaurant);
        hours.setDayOfWeek(DayOfWeek.MONDAY);
        hours.setOpenTime(LocalTime.of(9, 0));
        hours.setCloseTime(LocalTime.of(22, 0));
        hours.setDayOff(false);

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));
        when(workingHoursRepository.findByRestaurantId(1L)).thenReturn(List.of(hours));

        List<WorkingHoursDto> result = restaurantService.getWorkingHours(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(result.get(0).openTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    @DisplayName("getWorkingHours - should throw when restaurant not found")
    void getWorkingHours_shouldThrowWhenRestaurantNotFound() {
        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getWorkingHours(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(workingHoursRepository, never()).findByRestaurantId(anyLong());
    }

    @Test
    @DisplayName("isOpenAt - normal hours and boundaries")
    void isOpenAt_normalHours() {
        RestaurantWorkingHours hours = new RestaurantWorkingHours(restaurant, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(22, 0), false);
        when(workingHoursRepository.findByRestaurantId(1L)).thenReturn(List.of(hours));

        // Exact opening time (09:00:00) -> Open
        java.time.ZonedDateTime exactOpen = java.time.ZonedDateTime.of(2023, 10, 16, 9, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, exactOpen)).isTrue();

        // Daytime within window (12:00:00) -> Open
        java.time.ZonedDateTime midday = java.time.ZonedDateTime.of(2023, 10, 16, 12, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, midday)).isTrue();

        // Just before closing (21:59:59) -> Open
        java.time.ZonedDateTime justBeforeClose = java.time.ZonedDateTime.of(2023, 10, 16, 21, 59, 59, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, justBeforeClose)).isTrue();

        // Exact closing time (22:00:00) -> Closed
        java.time.ZonedDateTime exactClose = java.time.ZonedDateTime.of(2023, 10, 16, 22, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, exactClose)).isFalse();

        // Just before opening (08:59:59) -> Closed
        java.time.ZonedDateTime justBeforeOpen = java.time.ZonedDateTime.of(2023, 10, 16, 8, 59, 59, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, justBeforeOpen)).isFalse();

        // After close (23:00) -> Closed
        java.time.ZonedDateTime afterClose = java.time.ZonedDateTime.of(2023, 10, 16, 23, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, afterClose)).isFalse();
    }

    @Test
    @DisplayName("isOpenAt - overnight hours boundaries and daytime regression")
    void isOpenAt_overnightHours() {
        // Monday 18:00 to 02:00
        RestaurantWorkingHours hours = new RestaurantWorkingHours(restaurant, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(2, 0), false);
        when(workingHoursRepository.findByRestaurantId(1L)).thenReturn(List.of(hours));

        // Exact opening time Monday 18:00:00 -> Open
        java.time.ZonedDateTime exactOpen = java.time.ZonedDateTime.of(2023, 10, 16, 18, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, exactOpen)).isTrue();

        // Monday 20:00 -> Open
        java.time.ZonedDateTime time1 = java.time.ZonedDateTime.of(2023, 10, 16, 20, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul")); 
        assertThat(restaurantService.isOpenAt(1L, time1)).isTrue();

        // Monday 23:59:59 -> Open
        java.time.ZonedDateTime lateNight = java.time.ZonedDateTime.of(2023, 10, 16, 23, 59, 59, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, lateNight)).isTrue();

        // Tuesday 00:00:00 -> Open (overnight continuation)
        java.time.ZonedDateTime midnight = java.time.ZonedDateTime.of(2023, 10, 17, 0, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, midnight)).isTrue();

        // Tuesday 01:00 -> Open
        java.time.ZonedDateTime time2 = java.time.ZonedDateTime.of(2023, 10, 17, 1, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul")); 
        assertThat(restaurantService.isOpenAt(1L, time2)).isTrue();

        // Tuesday 01:59:59 (Just before closing) -> Open
        java.time.ZonedDateTime justBeforeClose = java.time.ZonedDateTime.of(2023, 10, 17, 1, 59, 59, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, justBeforeClose)).isTrue();

        // Exact closing time Tuesday 02:00:00 -> Closed
        java.time.ZonedDateTime exactClose = java.time.ZonedDateTime.of(2023, 10, 17, 2, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, exactClose)).isFalse();

        // Early morning: Tuesday 03:00:00 -> Closed
        java.time.ZonedDateTime earlyMorning = java.time.ZonedDateTime.of(2023, 10, 17, 3, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul")); 
        assertThat(restaurantService.isOpenAt(1L, earlyMorning)).isFalse();

        // Early morning: Tuesday 06:00:00 -> Closed
        java.time.ZonedDateTime morning = java.time.ZonedDateTime.of(2023, 10, 17, 6, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul")); 
        assertThat(restaurantService.isOpenAt(1L, morning)).isFalse();

        // Daytime: Monday 12:00:00 (before opening on Monday) -> Closed
        java.time.ZonedDateTime mondayMidday = java.time.ZonedDateTime.of(2023, 10, 16, 12, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, mondayMidday)).isFalse();

        // Daytime: Monday 17:59:59 (just before opening) -> Closed
        java.time.ZonedDateTime justBeforeOpen = java.time.ZonedDateTime.of(2023, 10, 16, 17, 59, 59, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, justBeforeOpen)).isFalse();

        // Daytime: Tuesday 12:00:00 -> Closed
        java.time.ZonedDateTime tuesdayMidday = java.time.ZonedDateTime.of(2023, 10, 17, 12, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"));
        assertThat(restaurantService.isOpenAt(1L, tuesdayMidday)).isFalse();
    }

    @Test
    @DisplayName("getRestaurantById - should report isOpenNow false when restaurant status is CLOSED")
    void getRestaurantById_whenStatusClosed_isOpenNowIsFalse() {
        restaurant.setStatus(RestaurantStatus.CLOSED);
        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponseDto result = restaurantService.getRestaurantById(1L);

        assertThat(result.isOpenNow()).isFalse();
    }

    @Test
    @DisplayName("isOpenAt - when restaurant has no working hours configured, defaults to open")
    void isOpenAt_whenNoHoursConfigured_returnsTrue() {
        when(workingHoursRepository.findByRestaurantId(1L)).thenReturn(List.of());

        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        assertThat(restaurantService.isOpenAt(1L, now)).isTrue();
    }
}