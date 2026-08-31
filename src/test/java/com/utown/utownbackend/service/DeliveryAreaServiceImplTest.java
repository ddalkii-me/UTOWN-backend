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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        city = new City();
        city.setId(1L);
        city.setName("Seoul");

        deliveryArea = new DeliveryArea();
        deliveryArea.setId(1L);
        deliveryArea.setCity(city);
        deliveryArea.setName("Gangnam");
    }
    @Test
    void createDeliveryArea_shouldCreateSuccessfully() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(1L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .existsByCityIdAndNameAndDeletedAtIsNull(
                        1L, "Gangnam"))
                .thenReturn(false);

        when(deliveryAreaRepository.save(any(DeliveryArea.class)))
                .thenReturn(deliveryArea);

        DeliveryAreaResponseDto result =
                deliveryAreaService.createDeliveryArea(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.cityId());
        assertEquals("Gangnam", result.name());

        verify(deliveryAreaRepository).save(any(DeliveryArea.class));
    }
    @Test
    void createDeliveryArea_shouldRejectWhenCityDoesNotExist() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(99L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> deliveryAreaService.createDeliveryArea(request)
        );

        verify(deliveryAreaRepository, never())
                .save(any(DeliveryArea.class));
    }
    @Test
    void createDeliveryArea_shouldRejectDuplicateName() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(1L, "Gangnam");

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .existsByCityIdAndNameAndDeletedAtIsNull(
                        1L, "Gangnam"))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> deliveryAreaService.createDeliveryArea(request)
        );

        verify(deliveryAreaRepository, never())
                .save(any(DeliveryArea.class));
    }
    @Test
    void getAllDeliveryAreas_shouldReturnActiveAreas() {

        when(deliveryAreaRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(deliveryArea));

        List<DeliveryAreaResponseDto> result =
                deliveryAreaService.getAllDeliveryAreas();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(1L, result.get(0).cityId());
        assertEquals("Gangnam", result.get(0).name());
    }
    @Test
    void getDeliveryAreasByCity_shouldReturnAreasForSelectedCity() {

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findAllByCityIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(deliveryArea));

        List<DeliveryAreaResponseDto> result =
                deliveryAreaService.getDeliveryAreasByCity(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).cityId());
        assertEquals("Gangnam", result.get(0).name());

        verify(deliveryAreaRepository)
                .findAllByCityIdAndDeletedAtIsNull(1L);
    }
    @Test
    void getDeliveryAreasByCity_shouldRejectWhenCityDoesNotExist() {

        when(cityRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> deliveryAreaService.getDeliveryAreasByCity(99L)
        );

        verify(deliveryAreaRepository, never())
                .findAllByCityIdAndDeletedAtIsNull(anyLong());
    }
    @Test
    void getDeliveryAreaById_shouldReturnActiveArea() {

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        DeliveryAreaResponseDto result =
                deliveryAreaService.getDeliveryAreaById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.cityId());
        assertEquals("Gangnam", result.name());
    }
    @Test
    void getDeliveryAreaById_shouldRejectWhenAreaDoesNotExist() {

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> deliveryAreaService.getDeliveryAreaById(99L)
        );
    }
    @Test
    void updateDeliveryArea_shouldUpdateSuccessfully() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(1L, "Gangnam Updated");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(
                        1L, "Gangnam Updated", 1L))
                .thenReturn(false);

        when(deliveryAreaRepository.save(deliveryArea))
                .thenReturn(deliveryArea);

        DeliveryAreaResponseDto result =
                deliveryAreaService.updateDeliveryArea(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.cityId());
        assertEquals("Gangnam Updated", result.name());

        verify(deliveryAreaRepository).save(deliveryArea);
    }
    @Test
    void updateDeliveryArea_shouldRejectDeletedCity() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(99L, "Gangnam Updated");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(cityRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> deliveryAreaService.updateDeliveryArea(1L, request)
        );

        verify(deliveryAreaRepository, never())
                .save(any(DeliveryArea.class));
    }
    @Test
    void updateDeliveryArea_shouldRejectDuplicateName() {

        DeliveryAreaRequestDto request =
                new DeliveryAreaRequestDto(1L, "Another Area");

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(
                        1L, "Another Area", 1L))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> deliveryAreaService.updateDeliveryArea(1L, request)
        );

        verify(deliveryAreaRepository, never())
                .save(any(DeliveryArea.class));
    }
    @Test
    void deleteDeliveryArea_shouldSoftDeleteSuccessfully() {

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(addressRepository
                .existsByDeliveryAreaIdAndDeletedAtIsNull(1L))
                .thenReturn(false);

        deliveryAreaService.deleteDeliveryArea(1L);

        assertNotNull(deliveryArea.getDeletedAt());

        verify(deliveryAreaRepository).save(deliveryArea);
    }
    @Test
    void deleteDeliveryArea_shouldRejectWhenActiveAddressExists() {

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(deliveryArea));

        when(addressRepository
                .existsByDeliveryAreaIdAndDeletedAtIsNull(1L))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> deliveryAreaService.deleteDeliveryArea(1L)
        );

        verify(deliveryAreaRepository, never())
                .save(any(DeliveryArea.class));
    }
    @Test
    void deleteDeliveryArea_shouldRejectWhenAreaDoesNotExist() {

        when(deliveryAreaRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> deliveryAreaService.deleteDeliveryArea(99L)
        );

        verify(addressRepository, never())
                .existsByDeliveryAreaIdAndDeletedAtIsNull(anyLong());
    }
}