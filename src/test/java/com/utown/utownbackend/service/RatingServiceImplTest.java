package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RatingRequestDto;
import com.utown.utownbackend.dto.RatingResponseDto;
import com.utown.utownbackend.entity.Order;
import com.utown.utownbackend.entity.OrderStatus;
import com.utown.utownbackend.entity.Rating;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.OrderRepository;
import com.utown.utownbackend.repository.RatingRepository;
import com.utown.utownbackend.repository.RestaurantRepository;
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
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private RatingServiceImpl ratingService;

    private User user;
    private Restaurant restaurant;
    private Order order;
    private Rating rating;
    private RatingRequestDto request;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);

        restaurant = new Restaurant();
        restaurant.setId(2L);

        order = new Order();
        order.setId(3L);
        order.setUser(user);
        order.setRestaurant(restaurant);
        order.setStatus(OrderStatus.DELIVERED);

        rating = new Rating();
        rating.setId(4L);
        rating.setUser(user);
        rating.setRestaurant(restaurant);
        rating.setOrder(order);
        rating.setScore(5);
        rating.setComment("Great service!");

        request = new RatingRequestDto(
                5,
                "Great service!"
        );
    }

    @Test
    void createRating_shouldCreateSuccessfully() {

        when(orderRepository.findById(3L))
                .thenReturn(Optional.of(order));

        when(ratingRepository
                .findByUserIdAndOrderIdAndDeletedAtIsNull(1L, 3L))
                .thenReturn(Optional.empty());

        when(ratingRepository.save(any(Rating.class)))
                .thenReturn(rating);

        RatingResponseDto response =
                ratingService.createRating(
                        3L,
                        request
                );

        assertNotNull(response);
        assertEquals(4L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(2L, response.restaurantId());
        assertEquals(3L, response.orderId());
        assertEquals(5, response.score());
        assertEquals("Great service!", response.comment());

        verify(orderRepository).findById(3L);

        verify(ratingRepository)
                .findByUserIdAndOrderIdAndDeletedAtIsNull(1L, 3L);

        verify(ratingRepository)
                .save(any(Rating.class));
    }

    @Test
    void createRating_shouldThrowWhenOrderNotFound() {

        when(orderRepository.findById(3L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.createRating(
                        3L,
                        request
                )
        );

        verifyNoInteractions(ratingRepository);
    }

    @Test
    void createRating_shouldThrowWhenOrderHasNoUser() {

        order.setUser(null);

        when(orderRepository.findById(3L))
                .thenReturn(Optional.of(order));

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.createRating(
                        3L,
                        request
                )
        );

        verifyNoInteractions(ratingRepository);
    }

    @Test
    void createRating_shouldThrowWhenOrderHasNoRestaurant() {

        order.setRestaurant(null);

        when(orderRepository.findById(3L))
                .thenReturn(Optional.of(order));

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.createRating(
                        3L,
                        request
                )
        );

        verifyNoInteractions(ratingRepository);
    }

    @Test
    void createRating_shouldThrowWhenOrderNotDelivered() {

        order.setStatus(OrderStatus.IN_PREPARATION);

        when(orderRepository.findById(3L))
                .thenReturn(Optional.of(order));

        assertThrows(
                ResourceConflictException.class,
                () -> ratingService.createRating(
                        3L,
                        request
                )
        );

        verifyNoInteractions(ratingRepository);
    }

    @Test
    void createRating_shouldThrowWhenOrderAlreadyRated() {

        when(orderRepository.findById(3L))
                .thenReturn(Optional.of(order));

        when(ratingRepository
                .findByUserIdAndOrderIdAndDeletedAtIsNull(1L, 3L))
                .thenReturn(Optional.of(rating));

        assertThrows(
                ResourceConflictException.class,
                () -> ratingService.createRating(
                        3L,
                        request
                )
        );

        verify(ratingRepository, never())
                .save(any(Rating.class));
    }

    @Test
    void getRatingsByRestaurant_shouldReturnRatings() {

        when(restaurantRepository.findById(2L))
                .thenReturn(Optional.of(restaurant));

        when(ratingRepository
                .findAllByRestaurantIdAndDeletedAtIsNull(2L))
                .thenReturn(List.of(rating));

        List<RatingResponseDto> response =
                ratingService.getRatingsByRestaurant(2L);

        assertEquals(1, response.size());
        assertEquals(4L, response.get(0).id());
        assertEquals(1L, response.get(0).userId());
        assertEquals(2L, response.get(0).restaurantId());
        assertEquals(3L, response.get(0).orderId());
        assertEquals(5, response.get(0).score());
        assertEquals(
                "Great service!",
                response.get(0).comment()
        );

        verify(restaurantRepository)
                .findById(2L);

        verify(ratingRepository)
                .findAllByRestaurantIdAndDeletedAtIsNull(2L);
    }

    @Test
    void getRatingsByRestaurant_shouldThrowWhenRestaurantNotFound() {

        when(restaurantRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.getRatingsByRestaurant(2L)
        );

        verifyNoInteractions(ratingRepository);
    }

    @Test
    void getRatingById_shouldReturnRating() {

        when(ratingRepository
                .findByIdAndDeletedAtIsNull(4L))
                .thenReturn(Optional.of(rating));

        RatingResponseDto response =
                ratingService.getRatingById(4L);

        assertNotNull(response);
        assertEquals(4L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(2L, response.restaurantId());
        assertEquals(3L, response.orderId());
        assertEquals(5, response.score());
        assertEquals(
                "Great service!",
                response.comment()
        );

        verify(ratingRepository)
                .findByIdAndDeletedAtIsNull(4L);
    }

    @Test
    void getRatingById_shouldThrowWhenRatingNotFound() {

        when(ratingRepository
                .findByIdAndDeletedAtIsNull(4L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.getRatingById(4L)
        );

        verify(ratingRepository)
                .findByIdAndDeletedAtIsNull(4L);
    }

    @Test
    void deleteRating_shouldSoftDeleteSuccessfully() {

        when(ratingRepository
                .findByIdAndDeletedAtIsNull(4L))
                .thenReturn(Optional.of(rating));

        ratingService.deleteRating(4L);

        assertNotNull(rating.getDeletedAt());

        verify(ratingRepository)
                .findByIdAndDeletedAtIsNull(4L);

        verify(ratingRepository)
                .save(rating);

        verify(ratingRepository, never())
                .delete(any(Rating.class));
    }

    @Test
    void deleteRating_shouldThrowWhenRatingNotFound() {

        when(ratingRepository
                .findByIdAndDeletedAtIsNull(4L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> ratingService.deleteRating(4L)
        );

        verify(ratingRepository)
                .findByIdAndDeletedAtIsNull(4L);

        verify(ratingRepository, never())
                .save(any(Rating.class));

        verify(ratingRepository, never())
                .delete(any(Rating.class));
    }
}