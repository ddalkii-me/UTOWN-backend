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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final DeliveryAreaRepository deliveryAreaRepository;

    @Transactional
    @Override
    public AddressResponseDto createAddress(AddressRequestDto request) {

        User user = userRepository
                .findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found"));

        City city = cityRepository
                .findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        DeliveryArea deliveryArea = deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(
                        request.deliveryAreaId(),
                        request.cityId()
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Delivery area not found for this city"));

        Address address = new Address();

        address.setUser(user);
        address.setCity(city);
        address.setDeliveryArea(deliveryArea);
        address.setLabel(request.label());
        address.setRecipientName(request.recipientName());
        address.setPhone(request.phone());
        address.setAddressLine(request.addressLine());
        address.setPostalCode(request.postalCode());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());

        Address savedAddress = addressRepository.save(address);

        return toDto(savedAddress);
    }

    @Override
    public List<AddressResponseDto> getAllAddresses() {

        return addressRepository
                .findAllByDeletedAtIsNull()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<AddressResponseDto> getAddressesByUser(Long userId) {

        userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found"));

        return addressRepository
                .findAllByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AddressResponseDto getAddressById(Long id) {

        Address address = addressRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Address not found"));

        return toDto(address);
    }

    @Transactional
    @Override
    public AddressResponseDto updateAddress(
            Long id,
            AddressRequestDto request) {

        Address address = addressRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Address not found"));

        User user = userRepository
                .findByIdAndDeletedAtIsNull(request.userId())
                .orElseThrow(() ->
                        new EntityNotFoundException("User not found"));

        City city = cityRepository
                .findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        DeliveryArea deliveryArea = deliveryAreaRepository
                .findByIdAndCityIdAndDeletedAtIsNull(
                        request.deliveryAreaId(),
                        request.cityId()
                )
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Delivery area not found for this city"));

        address.setUser(user);
        address.setCity(city);
        address.setDeliveryArea(deliveryArea);
        address.setLabel(request.label());
        address.setRecipientName(request.recipientName());
        address.setPhone(request.phone());
        address.setAddressLine(request.addressLine());
        address.setPostalCode(request.postalCode());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());

        Address updatedAddress = addressRepository.save(address);

        return toDto(updatedAddress);
    }

    @Transactional
    @Override
    public void deleteAddress(Long id) {

        Address address = addressRepository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Address not found"));

        address.setDeletedAt(LocalDateTime.now());

        addressRepository.save(address);
    }

    private AddressResponseDto toDto(Address address) {

        return new AddressResponseDto(
                address.getId(),
                address.getUser().getId(),
                address.getCity().getId(),
                address.getDeliveryArea().getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddressLine(),
                address.getPostalCode(),
                address.getLatitude(),
                address.getLongitude()
        );
    }
}