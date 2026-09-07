package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DeliveryAreaRequestDto;
import com.utown.utownbackend.dto.DeliveryAreaResponseDto;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.DeliveryArea;
import com.utown.utownbackend.exception.ResourceConflictException;
import com.utown.utownbackend.repository.AddressRepository;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.DeliveryAreaRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAreaServiceImplTest {

    @Mock
    private DeliveryAreaRepository deliveryAreaRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private DeliveryAreaServiceImpl deliveryAreaService;

    private City city;
    private DeliveryArea deliveryArea;

    @BeforeEach
    void setUp() {
        city = TestDataFactory.createCity(1L, "Seoul");
        deliveryArea = TestDataFactory.createDeliveryArea(1L, "Gangnam", city);
    }

    // ── createDeliveryArea ───────────────────────────────────────────

    @Test
    @DisplayName("createDeliveryArea - should create successfully")
    void createDeliveryArea_shouldCreateSuccessfully() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(1L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.existsByCityIdAndNameAndDeletedAtIsNull(1L, "Gangnam")).thenReturn(false);
        when(deliveryAreaRepository.save(any(DeliveryArea.class))).thenReturn(deliveryArea);

        DeliveryAreaResponseDto result = deliveryAreaService.createDeliveryArea(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.cityId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Gangnam");
        verify(deliveryAreaRepository).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("createDeliveryArea - should reject when city does not exist")
    void createDeliveryArea_shouldRejectWhenCityDoesNotExist() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(99L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.createDeliveryArea(request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("createDeliveryArea - should reject duplicate name within city")
    void createDeliveryArea_shouldRejectDuplicateName() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(1L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.existsByCityIdAndNameAndDeletedAtIsNull(1L, "Gangnam")).thenReturn(true);

        assertThatThrownBy(() -> deliveryAreaService.createDeliveryArea(request))
                .isInstanceOf(ResourceConflictException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    // ── getAllDeliveryAreas ───────────────────────────────────────────

    @Test
    @DisplayName("getAllDeliveryAreas - should return active areas")
    void getAllDeliveryAreas_shouldReturnActiveAreas() {
        when(deliveryAreaRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(deliveryArea));

        List<DeliveryAreaResponseDto> result = deliveryAreaService.getAllDeliveryAreas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).cityId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Gangnam");
    }

    // ── getDeliveryAreasByCity ────────────────────────────────────────

    @Test
    @DisplayName("getDeliveryAreasByCity - should return areas for selected city")
    void getDeliveryAreasByCity_shouldReturnAreasForSelectedCity() {
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.findAllByCityIdAndDeletedAtIsNull(1L)).thenReturn(List.of(deliveryArea));

        List<DeliveryAreaResponseDto> result = deliveryAreaService.getDeliveryAreasByCity(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cityId()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Gangnam");
        verify(deliveryAreaRepository).findAllByCityIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("getDeliveryAreasByCity - should reject when city does not exist")
    void getDeliveryAreasByCity_shouldRejectWhenCityDoesNotExist() {
        when(cityRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.getDeliveryAreasByCity(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(deliveryAreaRepository, never()).findAllByCityIdAndDeletedAtIsNull(anyLong());
    }

    // ── getDeliveryAreaById ──────────────────────────────────────────

    @Test
    @DisplayName("getDeliveryAreaById - should return active area")
    void getDeliveryAreaById_shouldReturnActiveArea() {
        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));

        DeliveryAreaResponseDto result = deliveryAreaService.getDeliveryAreaById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.cityId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Gangnam");
    }

    @Test
    @DisplayName("getDeliveryAreaById - should reject when area does not exist")
    void getDeliveryAreaById_shouldRejectWhenAreaDoesNotExist() {
        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.getDeliveryAreaById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateDeliveryArea ───────────────────────────────────────────

    @Test
    @DisplayName("updateDeliveryArea - should update successfully")
    void updateDeliveryArea_shouldUpdateSuccessfully() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(1L, "Gangnam Updated");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(1L, "Gangnam Updated", 1L)).thenReturn(false);
        when(deliveryAreaRepository.save(deliveryArea)).thenReturn(deliveryArea);

        DeliveryAreaResponseDto result = deliveryAreaService.updateDeliveryArea(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.cityId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Gangnam Updated");
        verify(deliveryAreaRepository).save(deliveryArea);
    }

    @Test
    @DisplayName("updateDeliveryArea - should reject nonexistent delivery area")
    void updateDeliveryArea_notFound_throwsNotFound() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(1L, "Gangnam Updated");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.updateDeliveryArea(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("updateDeliveryArea - should reject deleted city")
    void updateDeliveryArea_shouldRejectDeletedCity() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(99L, "Gangnam Updated");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(cityRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.updateDeliveryArea(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("updateDeliveryArea - should reject duplicate name")
    void updateDeliveryArea_shouldRejectDuplicateName() {
        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(1L, "Another Area");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(1L, "Another Area", 1L)).thenReturn(true);

        assertThatThrownBy(() -> deliveryAreaService.updateDeliveryArea(1L, request))
                .isInstanceOf(ResourceConflictException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("updateDeliveryArea - should reject city change when active address exists")
    void updateDeliveryArea_shouldRejectCityChangeWhenActiveAddressExists() {
        City newCity = new City();
        newCity.setId(2L);
        newCity.setName("Busan");

        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(2L, "Gangnam");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(cityRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(newCity));
        when(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> deliveryAreaService.updateDeliveryArea(1L, request))
                .isInstanceOf(ResourceConflictException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("updateDeliveryArea - should allow city change when no active address exists")
    void updateDeliveryArea_shouldAllowCityChangeWhenNoActiveAddressExists() {
        City newCity = new City();
        newCity.setId(2L);
        newCity.setName("Busan");

        DeliveryAreaRequestDto request = new DeliveryAreaRequestDto(2L, "Gangnam");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(cityRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(newCity));
        when(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(1L)).thenReturn(false);
        when(deliveryAreaRepository.existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(2L, "Gangnam", 1L)).thenReturn(false);
        when(deliveryAreaRepository.save(deliveryArea)).thenReturn(deliveryArea);

        DeliveryAreaResponseDto result = deliveryAreaService.updateDeliveryArea(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.cityId()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Gangnam");
        verify(deliveryAreaRepository).save(deliveryArea);
    }

    // ── deleteDeliveryArea ───────────────────────────────────────────

    @Test
    @DisplayName("deleteDeliveryArea - should soft-delete successfully")
    void deleteDeliveryArea_shouldSoftDeleteSuccessfully() {
        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(1L)).thenReturn(false);

        deliveryAreaService.deleteDeliveryArea(1L);

        assertThat(deliveryArea.getDeletedAt()).isNotNull();
        verify(deliveryAreaRepository).save(deliveryArea);
    }

    @Test
    @DisplayName("deleteDeliveryArea - should reject when active address exists")
    void deleteDeliveryArea_shouldRejectWhenActiveAddressExists() {
        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(deliveryArea));
        when(addressRepository.existsByDeliveryAreaIdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> deliveryAreaService.deleteDeliveryArea(1L))
                .isInstanceOf(ResourceConflictException.class);
        verify(deliveryAreaRepository, never()).save(any(DeliveryArea.class));
    }

    @Test
    @DisplayName("deleteDeliveryArea - should reject when area does not exist")
    void deleteDeliveryArea_shouldRejectWhenAreaDoesNotExist() {
        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryAreaService.deleteDeliveryArea(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).existsByDeliveryAreaIdAndDeletedAtIsNull(anyLong());
    }
}