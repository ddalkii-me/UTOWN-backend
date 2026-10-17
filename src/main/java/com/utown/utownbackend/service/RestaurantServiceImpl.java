package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.*;
import com.utown.utownbackend.entity.*;
import com.utown.utownbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.CacheManager;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantTypeRepository restaurantTypeRepository;
    private final CityRepository cityRepository;
    private final RestaurantWorkingHoursRepository workingHoursRepository;
    private final FileStorageService fileStorageService;
    private final RestaurantLogoUpdateService restaurantLogoUpdateService;
    private final CacheManager cacheManager;

    @Transactional
    @Override
    public RestaurantResponseDto createRestaurant(RestaurantRequestDto request) {
        log.info("Executing createRestaurant");

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        City city = cityRepository.findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

        Restaurant restaurant = new Restaurant();

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setMinimumOrderAmount(request.minimumOrderAmount());
        restaurant.setStatus(request.status());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toDto(savedRestaurant);
    }

    @Override
    public List<RestaurantResponseDto> getAllRestaurants() {
        log.info("Executing getAllRestaurants");

        List<Restaurant> restaurants =
                restaurantRepository.findAllByDeletedAtIsNull();

        if (restaurants.isEmpty()) {
            return List.of();
        }

        List<Long> restaurantIds = restaurants.stream().map(BaseEntity::getId).toList();
        List<RestaurantWorkingHours> allHours = workingHoursRepository.findByRestaurantIdIn(restaurantIds);
        Map<Long, List<RestaurantWorkingHours>> hoursMap = allHours.stream()
                .collect(Collectors.groupingBy(h -> h.getRestaurant().getId()));

        ZonedDateTime now = ZonedDateTime.now();
        return restaurants.stream()
                .map(restaurant -> {
                    boolean isOpenNow = restaurant.getStatus() == RestaurantStatus.OPEN
                            && isHoursOpenAt(hoursMap.get(restaurant.getId()), now);
                    return toDto(restaurant, isOpenNow);
                })
                .toList();
    }

    @Override
    public RestaurantResponseDto getRestaurantById(Long id) {
        log.info("Executing getRestaurantById with id={}", id);
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        return toDto(restaurant);
    }

    @Transactional
    @Override
    public RestaurantResponseDto updateRestaurant(
            Long id,
            RestaurantOwnerUpdateRequestDto request) {
        log.info("Executing updateRestaurant with id={}", id);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        City city = cityRepository.findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setMinimumOrderAmount(request.minimumOrderAmount());
        restaurant.setStatus(request.status());

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        evictRestaurantMenu(id);

        return toDto(updatedRestaurant);
    }
    @Transactional
    @Override
    public RestaurantResponseDto updateRestaurantAsAdmin(
            Long id,
            RestaurantAdminUpdateRequestDto request) {
        log.info("Executing updateRestaurantAsAdmin with id={}", id);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new EntityNotFoundException("Owner not found"));

        RestaurantType type = restaurantTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new EntityNotFoundException("Restaurant type not found"));

        City city = cityRepository.findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() -> new EntityNotFoundException("City not found"));

        restaurant.setOwner(owner);
        restaurant.setType(type);
        restaurant.setCity(city);
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setPhone(request.phone());
        restaurant.setLogoUrl(request.logoUrl());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setMinimumOrderAmount(request.minimumOrderAmount());
        restaurant.setStatus(request.status());

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        evictRestaurantMenu(id);

        return toDto(updatedRestaurant);
    }

    @Transactional
    @Override
    public void deleteRestaurant(Long id) {
        log.info("Executing deleteRestaurant with id={}", id);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        restaurant.setDeletedAt(LocalDateTime.now());

        restaurantRepository.save(restaurant);

        evictRestaurantMenu(id);
    }

    @Transactional
    @Override
    public void updateWorkingHourForDay(Long restaurantId, DayOfWeek dayOfWeek, WorkingHoursDto dto) {
        log.info("Executing updateWorkingHourForDay with restaurantId={}, dayOfWeek={}", restaurantId, dayOfWeek);

        Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        RestaurantWorkingHours entity = workingHoursRepository.findByRestaurantIdAndDayOfWeek(restaurantId, dayOfWeek)
                .orElseGet(RestaurantWorkingHours::new);

        entity.setRestaurant(restaurant);
        entity.setDayOfWeek(dayOfWeek);
        entity.setOpenTime(dto.openTime());
        entity.setCloseTime(dto.closeTime());
        entity.setDayOff(dto.dayOff());

        workingHoursRepository.save(entity);
    }


    @Override
    public List<WorkingHoursDto> getWorkingHours(Long restaurantId) {
        log.info("Executing getWorkingHours with restaurantId={}", restaurantId);

        restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        List<RestaurantWorkingHours> entities = workingHoursRepository.findByRestaurantId(restaurantId);

        return entities.stream()
                .map(entity -> new WorkingHoursDto(
                        entity.getDayOfWeek(),
                        entity.getOpenTime(),
                        entity.getCloseTime(),
                        entity.isDayOff()
                ))
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RestaurantResponseDto uploadRestaurantLogo(Long id, MultipartFile file) throws IOException {
        String logoKey = fileStorageService.uploadFile(file);

        return restaurantLogoUpdateService.updateLogoUrl(id, logoKey);
    }

    @Override
    public boolean isOpenAt(Long restaurantId, ZonedDateTime zonedDateTime) {
        List<RestaurantWorkingHours> allHours = workingHoursRepository.findByRestaurantId(restaurantId);
        return isHoursOpenAt(allHours, zonedDateTime);
    }

    public static boolean isHoursOpenAt(List<RestaurantWorkingHours> allHours, ZonedDateTime zonedDateTime) {
        if (allHours == null || allHours.isEmpty()) {
            // Restaurants without configured hours can still take orders
            return true;
        }

        ZonedDateTime seoulTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Seoul"));
        DayOfWeek currentDay = seoulTime.getDayOfWeek();
        LocalTime currentTime = seoulTime.toLocalTime();

        Optional<RestaurantWorkingHours> todayHoursOpt = allHours.stream()
                .filter(h -> h.getDayOfWeek() == currentDay)
                .findFirst();

        if (todayHoursOpt.isPresent()) {
            RestaurantWorkingHours today = todayHoursOpt.get();
            if (!today.isDayOff()) {
                if (today.getOpenTime() != null && today.getCloseTime() != null) {
                    if (today.getOpenTime().isBefore(today.getCloseTime())) {
                        if (!currentTime.isBefore(today.getOpenTime()) && currentTime.isBefore(today.getCloseTime())) {
                            return true;
                        }
                    } else if (today.getOpenTime().isAfter(today.getCloseTime())) {
                        // overnight, e.g. 18:00 to 02:00
                        if (!currentTime.isBefore(today.getOpenTime())) {
                            return true;
                        }
                    }
                }
            }
        }

        DayOfWeek previousDay = currentDay.minus(1);
        Optional<RestaurantWorkingHours> yesterdayHoursOpt = allHours.stream()
                .filter(h -> h.getDayOfWeek() == previousDay)
                .findFirst();

        if (yesterdayHoursOpt.isPresent()) {
            RestaurantWorkingHours yesterday = yesterdayHoursOpt.get();
            if (!yesterday.isDayOff()) {
                if (yesterday.getOpenTime() != null && yesterday.getCloseTime() != null) {
                    if (yesterday.getOpenTime().isAfter(yesterday.getCloseTime())) {
                        if (currentTime.isBefore(yesterday.getCloseTime())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void evictRestaurantMenu(Long restaurantId) {
        var menuCache = cacheManager.getCache("restaurantMenus");
        if (menuCache != null) {
            menuCache.evict(restaurantId);
        }
    }

    private RestaurantResponseDto toDto(Restaurant restaurant) {
        boolean isOpenNow = restaurant.getStatus() == RestaurantStatus.OPEN
                && isOpenAt(restaurant.getId(), ZonedDateTime.now());
        return toDto(restaurant, isOpenNow);
    }

    private RestaurantResponseDto toDto(Restaurant restaurant, boolean isOpenNow) {
        return new RestaurantResponseDto(
                restaurant.getId(),
                restaurant.getOwner().getId(),
                restaurant.getType().getId(),
                restaurant.getCity().getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getLogoUrl(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getMinimumOrderAmount(),
                restaurant.getStatus(),
                isOpenNow,
                restaurant.getAverageRating(),
                restaurant.getDeliveryTimeMinutes()
        );
    }
}