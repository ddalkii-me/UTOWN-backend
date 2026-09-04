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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteRestaurantServiceImpl
        implements FavoriteRestaurantService {

    private final FavoriteRestaurantRepository favoriteRestaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public FavoriteRestaurantResponseDto addFavorite(
            Long userId,
            Long restaurantId) {

        User user = userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        Restaurant restaurant = restaurantRepository
                .findByIdAndDeletedAtIsNull(restaurantId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Restaurant not found with id: " + restaurantId
                        )
                );

        FavoriteRestaurant existing =
                favoriteRestaurantRepository
                        .findByUserIdAndRestaurantId(
                                userId,
                                restaurantId
                        )
                        .orElse(null);

        if (existing != null) {

            if (existing.getDeletedAt() != null) {
                existing.setDeletedAt(null);

                FavoriteRestaurant restored =
                        favoriteRestaurantRepository.save(existing);

                return toResponseDto(restored);
            }

            throw new ResourceConflictException(
                    "Restaurant is already in favorites"
            );
        }

        FavoriteRestaurant favoriteRestaurant =
                new FavoriteRestaurant();

        favoriteRestaurant.setUser(user);
        favoriteRestaurant.setRestaurant(restaurant);

        FavoriteRestaurant saved =
                favoriteRestaurantRepository.save(favoriteRestaurant);

        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteRestaurantResponseDto> getFavoritesByUser(
            Long userId) {

        userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found with id: " + userId
                        )
                );

        return favoriteRestaurantRepository
                .findAllByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public void removeFavorite(
            Long userId,
            Long restaurantId) {

        FavoriteRestaurant favoriteRestaurant =
                favoriteRestaurantRepository
                        .findByUserIdAndRestaurantIdAndDeletedAtIsNull(
                                userId,
                                restaurantId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Restaurant is not in favorites"
                                )
                        );

        favoriteRestaurant.setDeletedAt(
                LocalDateTime.now()
        );

        favoriteRestaurantRepository.save(favoriteRestaurant);
    }

    private FavoriteRestaurantResponseDto toResponseDto(
            FavoriteRestaurant favoriteRestaurant) {

        return new FavoriteRestaurantResponseDto(
                favoriteRestaurant.getId(),
                favoriteRestaurant.getUser().getId(),
                favoriteRestaurant.getRestaurant().getId()
        );
    }
}