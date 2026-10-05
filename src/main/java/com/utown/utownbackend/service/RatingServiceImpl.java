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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;

    @Override
    public RatingResponseDto createRating(
            Long orderId,
            RatingRequestDto request
    ) {
        log.info("Executing createRating with orderId={}", orderId);

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Order not found with id: " + orderId
                        )
                );

        User user = order.getUser();
        Restaurant restaurant = order.getRestaurant();

        if (user == null) {
            throw new EntityNotFoundException(
                    "User associated with order not found."
            );
        }

        if (restaurant == null) {
            throw new EntityNotFoundException(
                    "Restaurant associated with order not found."
            );
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResourceConflictException(
                    "Only delivered orders can be rated."
            );
        }

        if (ratingRepository
                .findByUserIdAndOrderIdAndDeletedAtIsNull(
                        user.getId(),
                        orderId
                )
                .isPresent()) {

            throw new ResourceConflictException(
                    "This order has already been rated."
            );
        }

        Rating rating = new Rating();
        rating.setUser(user);
        rating.setRestaurant(restaurant);
        rating.setOrder(order);
        rating.setScore(request.score());
        rating.setComment(request.comment());

        Rating saved = ratingRepository.save(rating);

        updateRestaurantAverageRating(restaurant);

        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponseDto> getRatingsByRestaurant(
            Long restaurantId
    ) {
        log.info("Executing getRatingsByRestaurant with restaurantId={}", restaurantId);

        restaurantRepository
                .findById(restaurantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Restaurant not found with id: " + restaurantId
                        )
                );

        return ratingRepository
                .findAllByRestaurantIdAndDeletedAtIsNull(restaurantId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponseDto getRatingById(Long id) {
        log.info("Executing getRatingById with id={}", id);

        Rating rating = ratingRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rating not found with id: " + id
                        )
                );

        return toResponseDto(rating);
    }

    @Override
    public void deleteRating(Long id) {
        log.info("Executing deleteRating with id={}", id);

        Rating rating = ratingRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Rating not found"
                        )
                );

        rating.setDeletedAt(LocalDateTime.now());

        ratingRepository.save(rating);

        updateRestaurantAverageRating(rating.getRestaurant());
    }

    private void updateRestaurantAverageRating(Restaurant restaurant) {
        Double avg = ratingRepository.getAverageScoreByRestaurantId(restaurant.getId());
        if (avg == null) {
            restaurant.setAverageRating(java.math.BigDecimal.ZERO);
        } else {
            restaurant.setAverageRating(java.math.BigDecimal.valueOf(avg).setScale(2, java.math.RoundingMode.HALF_UP));
        }
        restaurantRepository.save(restaurant);
    }

    private RatingResponseDto toResponseDto(Rating rating) {

        return new RatingResponseDto(
                rating.getId(),
                rating.getUser().getId(),
                rating.getRestaurant().getId(),
                rating.getOrder().getId(),
                rating.getScore(),
                rating.getComment()
        );
    }
}