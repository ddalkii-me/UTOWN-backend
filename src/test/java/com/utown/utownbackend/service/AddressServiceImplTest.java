package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AddressRequestDto;
import com.utown.utownbackend.dto.AddressResponseDto;
import com.utown.utownbackend.entity.Address;
import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.DeliveryArea;
import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.repository.AddressRepository;
import com.utown.utownbackend.repository.CityRepository;
import com.utown.utownbackend.repository.DeliveryAreaRepository;
import com.utown.utownbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private DeliveryAreaRepository deliveryAreaRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User user;
    private City city;
    private DeliveryArea deliveryArea;
    private Address address;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);

        city = new City();
        city.setId(1L);

        deliveryArea = new DeliveryArea();
        deliveryArea.setId(1L);
        deliveryArea.setCity(city);
        deliveryArea.setName("Gangnam");

        address = new Address();
        address.setId(1L);
        address.setUser(user);
        address.setCity(city);
        address.setDeliveryArea(deliveryArea);
        address.setLabel("Home");
        address.setRecipientName("Test User");
        address.setPhone("010-1234-5678");
        address.setAddressLine("123 Gangnam Street");
        address.setPostalCode("06000");
        address.setLatitude(new BigDecimal("37.4979"));
        address.setLongitude(new BigDecimal("127.0276"));
    }

    private AddressRequestDto createRequest() {

        return new AddressRequestDto(
                1L,
                1L,
                1L,
                "Home",
                "Test User",
                "010-1234-5678",
                "123 Gangnam Street",
                "06000",
                new BigDecimal("37.4979"),
                new BigDecimal("127.0276")
        );
    }
    @Test
    void createAddress_shouldCreateSuccessfully() {

        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(deliveryArea));

        when(addressRepository.save(any(Address.class)))
                .thenReturn(address);

        AddressResponseDto result =
                addressService.createAddress(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(1L, result.userId());
        assertEquals(1L, result.cityId());
        assertEquals(1L, result.deliveryAreaId());
        assertEquals("Home", result.label());
        assertEquals("Test User", result.recipientName());

        verify(addressRepository).save(any(Address.class));
    }
    @Test
    void createAddress_shouldRejectDeliveryAreaFromDifferentCity() {

        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.createAddress(request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void createAddress_shouldRejectDeletedOrNonexistentUser() {

        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.createAddress(request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void createAddress_shouldRejectDeletedOrNonexistentCity() {

        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.createAddress(request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void createAddress_shouldRejectDeletedOrNonexistentDeliveryArea() {

        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.createAddress(request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void getAllAddresses_shouldReturnOnlyActiveAddresses() {

        when(addressRepository.findAllByDeletedAtIsNull())
                .thenReturn(List.of(address));

        List<AddressResponseDto> result =
                addressService.getAllAddresses();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Home", result.get(0).label());

        verify(addressRepository).findAllByDeletedAtIsNull();
    }
    @Test
    void getAddressesByUser_shouldReturnActiveAddresses() {

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(addressRepository.findAllByUserIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(address));

        List<AddressResponseDto> result =
                addressService.getAddressesByUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());

        verify(addressRepository)
                .findAllByUserIdAndDeletedAtIsNull(1L);
    }
    @Test
    void getAddressesByUser_shouldRejectDeletedOrNonexistentUser() {

        when(userRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.getAddressesByUser(99L)
        );

        verify(addressRepository, never())
                .findAllByUserIdAndDeletedAtIsNull(99L);
    }
    @Test
    void getAddressById_shouldReturnActiveAddress() {

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        AddressResponseDto result =
                addressService.getAddressById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Home", result.label());
        assertEquals("Test User", result.recipientName());

        verify(addressRepository)
                .findByIdAndDeletedAtIsNull(1L);
    }
    @Test
    void getAddressById_shouldRejectDeletedOrNonexistentAddress() {

        when(addressRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.getAddressById(99L)
        );
    }
    @Test
    void updateAddress_shouldUpdateSuccessfully() {

        AddressRequestDto request = new AddressRequestDto(
                1L,
                1L,
                1L,
                "Work",
                "Updated User",
                "010-9999-9999",
                "456 Updated Street",
                "06100",
                new BigDecimal("37.5000"),
                new BigDecimal("127.0300")
        );

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.of(deliveryArea));

        when(addressRepository.save(address))
                .thenReturn(address);

        AddressResponseDto result =
                addressService.updateAddress(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Work", result.label());
        assertEquals("Updated User", result.recipientName());
        assertEquals("456 Updated Street", result.addressLine());

        verify(addressRepository).save(address);
    }
    @Test
    void updateAddress_shouldRejectDeletedOrNonexistentUser() {

        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.updateAddress(1L, request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void updateAddress_shouldRejectDeletedOrNonexistentCity() {

        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.updateAddress(1L, request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void updateAddress_shouldRejectDeliveryAreaFromDifferentCity() {

        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        when(cityRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(city));

        when(deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.updateAddress(1L, request)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
    @Test
    void deleteAddress_shouldSoftDeleteSuccessfully() {

        when(addressRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(address));

        addressService.deleteAddress(1L);

        assertNotNull(address.getDeletedAt());

        verify(addressRepository).save(address);
    }
    @Test
    void deleteAddress_shouldRejectNonexistentOrDeletedAddress() {

        when(addressRepository.findByIdAndDeletedAtIsNull(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> addressService.deleteAddress(99L)
        );

        verify(addressRepository, never())
                .save(any(Address.class));
    }
}