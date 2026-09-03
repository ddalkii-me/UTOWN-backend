package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantDeliveryAreaResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.DeliveryArea;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantDeliveryArea;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.DeliveryAreaRepository;
import com.utown.utownbackend.repository.RestaurantDeliveryAreaRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.utown.utownbackend.service.impl.RestaurantDeliveryAreaServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantDeliveryAreaServiceImplTest {

    @Mock
    private RestaurantDeliveryAreaRepository restaurantDeliveryAreaRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private DeliveryAreaRepository deliveryAreaRepository;

    @InjectMocks
    private RestaurantDeliveryAreaServiceImpl restaurantDeliveryAreaService;

    private City city;
    private City differentCity;

    private Restaurant restaurant;

    private DeliveryArea deliveryArea;
    private DeliveryArea differentCityDeliveryArea;

    private RestaurantDeliveryArea restaurantDeliveryArea;

    @BeforeEach
    void setUp() {

        city = new City();
        city.setId(1L);

        differentCity = new City();
        differentCity.setId(2L);

        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setCity(city);

        deliveryArea = new DeliveryArea();
        deliveryArea.setId(1L);
        deliveryArea.setCity(city);
        deliveryArea.setName("Gangnam");

        differentCityDeliveryArea = new DeliveryArea();
        differentCityDeliveryArea.setId(2L);
        differentCityDeliveryArea.setCity(differentCity);
        differentCityDeliveryArea.setName("Haeundae");

        restaurantDeliveryArea =
                new RestaurantDeliveryArea();

        restaurantDeliveryArea.setId(1L);
        restaurantDeliveryArea.setRestaurant(restaurant);
        restaurantDeliveryArea.setDeliveryArea(deliveryArea);
    }

    @Test
    void addDeliveryArea_shouldCreateSuccessfully() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(restaurantDeliveryAreaRepository
                .findByRestaurantIdAndDeliveryAreaId(1L, 1L))
                .thenReturn(Optional.empty());

        when(restaurantDeliveryAreaRepository
                .save(any(RestaurantDeliveryArea.class)))
                .thenReturn(restaurantDeliveryArea);

        RestaurantDeliveryAreaResponseDto result =
                restaurantDeliveryAreaService
                        .addDeliveryArea(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.restaurantId());
        assertEquals(1L, result.deliveryAreaId());
        assertEquals("Gangnam", result.deliveryAreaName());

        verify(restaurantDeliveryAreaRepository)
                .save(any(RestaurantDeliveryArea.class));
    }

    @Test
    void addDeliveryArea_shouldRejectNonexistentOrDeletedRestaurant() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantDeliveryAreaService
                        .addDeliveryArea(99L, 1L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .save(any(RestaurantDeliveryArea.class));
    }

    @Test
    void addDeliveryArea_shouldRejectNonexistentOrDeletedDeliveryArea() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantDeliveryAreaService
                        .addDeliveryArea(1L, 99L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .save(any(RestaurantDeliveryArea.class));
    }

    @Test
    void addDeliveryArea_shouldRejectDeliveryAreaFromDifferentCity() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(differentCityDeliveryArea));

        assertThrows(
                ResourceConflictException.class,
                () -> restaurantDeliveryAreaService
                        .addDeliveryArea(1L, 2L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .save(any(RestaurantDeliveryArea.class));
    }

    @Test
    void addDeliveryArea_shouldRejectDuplicateActiveRelationship() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(restaurantDeliveryAreaRepository
                .findByRestaurantIdAndDeliveryAreaId(1L, 1L))
                .thenReturn(Optional.of(restaurantDeliveryArea));

        assertThrows(
                ResourceConflictException.class,
                () -> restaurantDeliveryAreaService
                        .addDeliveryArea(1L, 1L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .save(any(RestaurantDeliveryArea.class));
    }

    @Test
    void addDeliveryArea_shouldRestoreSoftDeletedRelationship() {

        restaurantDeliveryArea.setDeletedAt(
                LocalDateTime.now()
        );

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(restaurantDeliveryAreaRepository
                .findByRestaurantIdAndDeliveryAreaId(1L, 1L))
                .thenReturn(Optional.of(restaurantDeliveryArea));

        when(restaurantDeliveryAreaRepository
                .save(restaurantDeliveryArea))
                .thenReturn(restaurantDeliveryArea);

        RestaurantDeliveryAreaResponseDto result =
                restaurantDeliveryAreaService
                        .addDeliveryArea(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.restaurantId());
        assertEquals(1L, result.deliveryAreaId());
        assertEquals("Gangnam", result.deliveryAreaName());

        assertNull(restaurantDeliveryArea.getDeletedAt());

        verify(restaurantDeliveryAreaRepository)
                .save(restaurantDeliveryArea);
    }

    @Test
    void getDeliveryAreasByRestaurant_shouldReturnActiveRelationships() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(restaurant));

        when(restaurantDeliveryAreaRepository
                .findAllByRestaurantIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(restaurantDeliveryArea));

        List<RestaurantDeliveryAreaResponseDto> result =
                restaurantDeliveryAreaService
                        .getDeliveryAreasByRestaurant(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(1L, result.get(0).restaurantId());
        assertEquals(1L, result.get(0).deliveryAreaId());
        assertEquals(
                "Gangnam",
                result.get(0).deliveryAreaName()
        );

        verify(restaurantDeliveryAreaRepository)
                .findAllByRestaurantIdAndDeletedAtIsNull(1L);
    }

    @Test
    void getDeliveryAreasByRestaurant_shouldRejectNonexistentOrDeletedRestaurant() {

        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantDeliveryAreaService
                        .getDeliveryAreasByRestaurant(99L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .findAllByRestaurantIdAndDeletedAtIsNull(99L);
    }

    @Test
    void removeDeliveryArea_shouldSoftDeleteSuccessfully() {

        when(restaurantDeliveryAreaRepository
                .findByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
                        1L,
                        1L
                ))
                .thenReturn(Optional.of(restaurantDeliveryArea));

        restaurantDeliveryAreaService
                .removeDeliveryArea(1L, 1L);

        assertNotNull(
                restaurantDeliveryArea.getDeletedAt()
        );

        verify(restaurantDeliveryAreaRepository)
                .save(restaurantDeliveryArea);
    }

    @Test
    void removeDeliveryArea_shouldRejectNonexistentOrDeletedRelationship() {

        when(restaurantDeliveryAreaRepository
                .findByRestaurantIdAndDeliveryAreaIdAndDeletedAtIsNull(
                        99L,
                        99L
                ))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> restaurantDeliveryAreaService
                        .removeDeliveryArea(99L, 99L)
        );

        verify(restaurantDeliveryAreaRepository, never())
                .save(any(RestaurantDeliveryArea.class));
    }
}