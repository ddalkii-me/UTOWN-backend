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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.utown.utownbackend.util.TestDataFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        user = TestDataFactory.createUser(1L);
        city = TestDataFactory.createCity(1L, "Seoul");
        deliveryArea = TestDataFactory.createDeliveryArea(1L, "Gangnam", city);
        address = TestDataFactory.createAddress(1L, user, city, deliveryArea);
        address.setLabel("Home");
        address.setRecipientName("Test User");
        address.setAddressLine("123 Gangnam Street");
    }

    private AddressRequestDto createRequest() {
        return new AddressRequestDto(
                1L, 1L, 1L,
                "Home", "Test User", "010-1234-5678",
                "123 Gangnam Street", "06000",
                new BigDecimal("37.4979"), new BigDecimal("127.0276")
        );
    }

    // ── createAddress ────────────────────────────────────────────────

    @Test
    @DisplayName("createAddress - should create successfully")
    void createAddress_shouldCreateSuccessfully() {
        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(1L, 1L)).thenReturn(Optional.of(deliveryArea));
        when(addressRepository.save(any(Address.class))).thenReturn(address);

        AddressResponseDto result = addressService.createAddress(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.cityId()).isEqualTo(1L);
        assertThat(result.deliveryAreaId()).isEqualTo(1L);
        assertThat(result.label()).isEqualTo("Home");
        assertThat(result.recipientName()).isEqualTo("Test User");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress - should reject delivery area from different city")
    void createAddress_shouldRejectDeliveryAreaFromDifferentCity() {
        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.createAddress(request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress - should reject deleted or nonexistent user")
    void createAddress_shouldRejectDeletedOrNonexistentUser() {
        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.createAddress(request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress - should reject deleted or nonexistent city")
    void createAddress_shouldRejectDeletedOrNonexistentCity() {
        AddressRequestDto request = createRequest();

        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.createAddress(request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    // ── getAllAddresses ───────────────────────────────────────────────

    @Test
    @DisplayName("getAllAddresses - should return only active addresses")
    void getAllAddresses_shouldReturnOnlyActiveAddresses() {
        when(addressRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(address));

        List<AddressResponseDto> result = addressService.getAllAddresses();

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).label()).isEqualTo("Home");
        verify(addressRepository).findAllByDeletedAtIsNull();
    }

    // ── getAddressesByUser ───────────────────────────────────────────

    @Test
    @DisplayName("getAddressesByUser - should return active addresses for user")
    void getAddressesByUser_shouldReturnActiveAddresses() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findAllByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(address));

        List<AddressResponseDto> result = addressService.getAddressesByUser(1L);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        verify(addressRepository).findAllByUserIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("getAddressesByUser - should reject deleted or nonexistent user")
    void getAddressesByUser_shouldRejectDeletedOrNonexistentUser() {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressesByUser(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).findAllByUserIdAndDeletedAtIsNull(99L);
    }

    // ── getAddressById ───────────────────────────────────────────────

    @Test
    @DisplayName("getAddressById - should return active address")
    void getAddressById_shouldReturnActiveAddress() {
        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));

        AddressResponseDto result = addressService.getAddressById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.label()).isEqualTo("Home");
        assertThat(result.recipientName()).isEqualTo("Test User");
        verify(addressRepository).findByIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("getAddressById - should reject deleted or nonexistent address")
    void getAddressById_shouldRejectDeletedOrNonexistentAddress() {
        when(addressRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddressById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── updateAddress ────────────────────────────────────────────────

    @Test
    @DisplayName("updateAddress - should update successfully")
    void updateAddress_shouldUpdateSuccessfully() {
        AddressRequestDto request = new AddressRequestDto(
                1L, 1L, 1L,
                "Work", "Updated User", "010-9999-9999",
                "456 Updated Street", "06100",
                new BigDecimal("37.5000"), new BigDecimal("127.0300")
        );

        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(1L, 1L)).thenReturn(Optional.of(deliveryArea));
        when(addressRepository.save(address)).thenReturn(address);

        AddressResponseDto result = addressService.updateAddress(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.label()).isEqualTo("Work");
        assertThat(result.recipientName()).isEqualTo("Updated User");
        assertThat(result.addressLine()).isEqualTo("456 Updated Street");
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("updateAddress - should reject deleted or nonexistent user")
    void updateAddress_shouldRejectDeletedOrNonexistentUser() {
        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("updateAddress - should reject deleted or nonexistent city")
    void updateAddress_shouldRejectDeletedOrNonexistentCity() {
        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("updateAddress - should reject delivery area from different city")
    void updateAddress_shouldRejectDeliveryAreaFromDifferentCity() {
        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(cityRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(city));
        when(deliveryAreaRepository.findByIdAndCityIdAndDeletedAtIsNull(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(1L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    @DisplayName("updateAddress - should reject nonexistent or deleted address")
    void updateAddress_shouldRejectDeletedOrNonexistentAddress() {
        AddressRequestDto request = createRequest();

        when(addressRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.updateAddress(99L, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }

    // ── deleteAddress ────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAddress - should soft-delete successfully")
    void deleteAddress_shouldSoftDeleteSuccessfully() {
        when(addressRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(address));

        addressService.deleteAddress(1L);

        assertThat(address.getDeletedAt()).isNotNull();
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("deleteAddress - should reject nonexistent or deleted address")
    void deleteAddress_shouldRejectNonexistentOrDeletedAddress() {
        when(addressRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.deleteAddress(99L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(addressRepository, never()).save(any(Address.class));
    }
}