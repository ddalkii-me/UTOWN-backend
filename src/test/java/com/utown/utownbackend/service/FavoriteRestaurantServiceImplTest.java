package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.FavoriteRestaurantResponseDto;
import com.utown.utownbackend.entity.FavoriteRestaurant;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.FavoriteRestaurantRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteRestaurantServiceImplTest {

    @Mock
    private FavoriteRestaurantRepository favoriteRestaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private FavoriteRestaurantServiceImpl favoriteRestaurantService;

    private User user;
    private Restaurant restaurant;
    private FavoriteRestaurant favoriteRestaurant;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setPhone("01012345678");

        restaurant = new Restaurant();
        restaurant.setId(2L);
        restaurant.setName("Test Restaurant");

        favoriteRestaurant = new FavoriteRestaurant();
        favoriteRestaurant.setId(3L);
        favoriteRestaurant.setUser(user);
        favoriteRestaurant.setRestaurant(restaurant);
    }

    @Test
    void addFavorite_shouldCreateSuccessfully() {

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(restaurant));

        when(favoriteRestaurantRepository
                .findByUserIdAndRestaurantId(1L, 2L))
                .thenReturn(Optional.empty());

        when(favoriteRestaurantRepository.save(
                any(FavoriteRestaurant.class)))
                .thenReturn(favoriteRestaurant);

        FavoriteRestaurantResponseDto result =
                favoriteRestaurantService.addFavorite(1L, 2L);

        assertNotNull(result);
        assertEquals(3L, result.id());
        assertEquals(1L, result.userId());
        assertEquals(2L, result.restaurantId());

        verify(favoriteRestaurantRepository)
                .save(any(FavoriteRestaurant.class));
    }

    @Test
    void addFavorite_shouldRejectWhenUserDoesNotExist() {

        when(userRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> favoriteRestaurantService.addFavorite(99L, 2L)
        );

        verify(restaurantRepository, never())
                .findByIdAndDeletedAtIsNull(anyLong());

        verify(favoriteRestaurantRepository, never())
                .save(any(FavoriteRestaurant.class));
    }

    @Test
    void addFavorite_shouldRejectWhenRestaurantDoesNotExist() {

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> favoriteRestaurantService.addFavorite(1L, 99L)
        );

        verify(favoriteRestaurantRepository, never())
                .save(any(FavoriteRestaurant.class));
    }

    @Test
    void addFavorite_shouldRejectDuplicateActiveFavorite() {

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(restaurant));

        when(favoriteRestaurantRepository
                .findByUserIdAndRestaurantId(1L, 2L))
                .thenReturn(Optional.of(favoriteRestaurant));

        assertThrows(
                ResourceConflictException.class,
                () -> favoriteRestaurantService.addFavorite(1L, 2L)
        );

        verify(favoriteRestaurantRepository, never())
                .save(any(FavoriteRestaurant.class));
    }

    @Test
    void addFavorite_shouldRestoreSoftDeletedFavorite() {

        favoriteRestaurant.setDeletedAt(
                java.time.LocalDateTime.now()
        );

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(restaurantRepository.findByIdAndDeletedAtIsNull(2L))
                .thenReturn(Optional.of(restaurant));

        when(favoriteRestaurantRepository
                .findByUserIdAndRestaurantId(1L, 2L))
                .thenReturn(Optional.of(favoriteRestaurant));

        when(favoriteRestaurantRepository.save(favoriteRestaurant))
                .thenReturn(favoriteRestaurant);

        FavoriteRestaurantResponseDto result =
                favoriteRestaurantService.addFavorite(1L, 2L);

        assertNotNull(result);
        assertNull(favoriteRestaurant.getDeletedAt());

        verify(favoriteRestaurantRepository)
                .save(favoriteRestaurant);
    }

    @Test
    void getFavoritesByUser_shouldReturnActiveFavorites() {

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(favoriteRestaurantRepository
                .findAllByUserIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(favoriteRestaurant));

        List<FavoriteRestaurantResponseDto> result =
                favoriteRestaurantService.getFavoritesByUser(1L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).id());
        assertEquals(1L, result.get(0).userId());
        assertEquals(2L, result.get(0).restaurantId());
    }

    @Test
    void getFavoritesByUser_shouldRejectWhenUserDoesNotExist() {

        when(userRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> favoriteRestaurantService.getFavoritesByUser(99L)
        );

        verify(favoriteRestaurantRepository, never())
                .findAllByUserIdAndDeletedAtIsNull(anyLong());
    }

    @Test
    void removeFavorite_shouldSoftDeleteSuccessfully() {

        when(favoriteRestaurantRepository
                .findByUserIdAndRestaurantIdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.of(favoriteRestaurant));

        favoriteRestaurantService.removeFavorite(1L, 2L);

        assertNotNull(favoriteRestaurant.getDeletedAt());

        verify(favoriteRestaurantRepository)
                .save(favoriteRestaurant);
    }

    @Test
    void removeFavorite_shouldRejectWhenFavoriteDoesNotExist() {

        when(favoriteRestaurantRepository
                .findByUserIdAndRestaurantIdAndDeletedAtIsNull(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> favoriteRestaurantService.removeFavorite(1L, 2L)
        );

        verify(favoriteRestaurantRepository, never())
                .save(any(FavoriteRestaurant.class));
    }
}