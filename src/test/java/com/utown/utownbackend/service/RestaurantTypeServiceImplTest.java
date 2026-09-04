package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.RestaurantTypeRequestDto;
import com.utown.utownbackend.dto.RestaurantTypeResponseDto;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.repository.RestaurantTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utown.utownbackend.util.TestDataFactory;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTypeServiceImplTest {

    @Mock
    private RestaurantTypeRepository restaurantTypeRepository;

    @InjectMocks
    private RestaurantTypeServiceImpl restaurantTypeService;

    private RestaurantType restaurantType;
    private RestaurantTypeRequestDto requestDto;

    @BeforeEach
    void setUp() {
        restaurantType = TestDataFactory.createRestaurantType(1L, "Fast Food");
        restaurantType.setDescription("Quick meals");

        requestDto = new RestaurantTypeRequestDto("Fast Food", "Quick meals");
    }

    @Test
    @DisplayName("createRestaurantType - should return saved type")
    void createRestaurantType_shouldReturnSavedType() {
        when(restaurantTypeRepository.save(any(RestaurantType.class))).thenReturn(restaurantType);

        RestaurantTypeResponseDto result = restaurantTypeService.createRestaurantType(requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Fast Food");
        verify(restaurantTypeRepository).save(any(RestaurantType.class));
    }

    @Test
    @DisplayName("getAllRestaurantTypes - should return list")
    void getAllRestaurantTypes_shouldReturnList() {
        when(restaurantTypeRepository.findAll()).thenReturn(List.of(restaurantType));

        List<RestaurantTypeResponseDto> result = restaurantTypeService.getAllRestaurantTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Fast Food");
    }

    @Test
    @DisplayName("getRestaurantTypeById - should return type when found")
    void getRestaurantTypeById_shouldReturnType() {
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.of(restaurantType));

        RestaurantTypeResponseDto result = restaurantTypeService.getRestaurantTypeById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRestaurantTypeById - should throw when not found")
    void getRestaurantTypeById_shouldThrowWhenNotFound() {
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTypeService.getRestaurantTypeById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("updateRestaurantType - should update and return type")
    void updateRestaurantType_shouldUpdateAndReturnType() {
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.of(restaurantType));
        when(restaurantTypeRepository.save(any(RestaurantType.class))).thenReturn(restaurantType);

        RestaurantTypeResponseDto result = restaurantTypeService.updateRestaurantType(1L, requestDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Fast Food");
        verify(restaurantTypeRepository).save(restaurantType);
    }

    @Test
    @DisplayName("updateRestaurantType - should throw when not found")
    void updateRestaurantType_shouldThrowWhenNotFound() {
        when(restaurantTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTypeService.updateRestaurantType(99L, requestDto))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantTypeRepository, never()).save(any(RestaurantType.class));
    }

    @Test
    @DisplayName("deleteRestaurantType - should delete type")
    void deleteRestaurantType_shouldDelete() {
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.of(restaurantType));

        restaurantTypeService.deleteRestaurantType(1L);

        verify(restaurantTypeRepository).delete(restaurantType);
    }

    @Test
    @DisplayName("deleteRestaurantType - should throw when not found")
    void deleteRestaurantType_shouldThrowWhenNotFound() {
        when(restaurantTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantTypeService.deleteRestaurantType(1L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(restaurantTypeRepository, never()).delete(any(RestaurantType.class));
    }
}
