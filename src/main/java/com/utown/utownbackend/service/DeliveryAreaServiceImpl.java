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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryAreaServiceImpl implements DeliveryAreaService {

    private final DeliveryAreaRepository deliveryAreaRepository;
    private final CityRepository cityRepository;
    private final AddressRepository addressRepository;

    @Transactional
    @Override
    public DeliveryAreaResponseDto createDeliveryArea(
            DeliveryAreaRequestDto request) {

        City city = cityRepository
                .findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));
        if (deliveryAreaRepository
                .existsByCityIdAndNameAndDeletedAtIsNull(
                        request.cityId(),
                        request.name())) {

            throw new ResourceConflictException(
                    "Delivery area with this name already exists in this city."
            );
        }

        DeliveryArea deliveryArea = new DeliveryArea();

        deliveryArea.setCity(city);
        deliveryArea.setName(request.name());

        DeliveryArea savedDeliveryArea =
                deliveryAreaRepository.save(deliveryArea);

        return toDto(savedDeliveryArea);
    }

    @Override
    public List<DeliveryAreaResponseDto> getAllDeliveryAreas() {

        List<DeliveryArea> deliveryAreas =
                deliveryAreaRepository.findAllByDeletedAtIsNull();

        return deliveryAreas.stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<DeliveryAreaResponseDto> getDeliveryAreasByCity(
            Long cityId) {

        cityRepository.findByIdAndDeletedAtIsNull(cityId)
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));

        return deliveryAreaRepository
                .findAllByCityIdAndDeletedAtIsNull(cityId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public DeliveryAreaResponseDto getDeliveryAreaById(
            Long id) {

        DeliveryArea deliveryArea =
                deliveryAreaRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Delivery area not found"));

        return toDto(deliveryArea);
    }

    @Transactional
    @Override
    public DeliveryAreaResponseDto updateDeliveryArea(
            Long id,
            DeliveryAreaRequestDto request) {

        DeliveryArea deliveryArea =
                deliveryAreaRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Delivery area not found"));

        City city = cityRepository
                .findByIdAndDeletedAtIsNull(request.cityId())
                .orElseThrow(() ->
                        new EntityNotFoundException("City not found"));
        if (deliveryAreaRepository
                .existsByCityIdAndNameAndIdNotAndDeletedAtIsNull(
                        request.cityId(),
                        request.name(),
                        id)) {

            throw new ResourceConflictException(
                    "Delivery area with this name already exists in this city."
            );
        }

        deliveryArea.setCity(city);
        deliveryArea.setName(request.name());

        DeliveryArea updatedDeliveryArea =
                deliveryAreaRepository.save(deliveryArea);

        return toDto(updatedDeliveryArea);
    }

    @Transactional
    @Override
    public void deleteDeliveryArea(Long id) {

        DeliveryArea deliveryArea =
                deliveryAreaRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Delivery area not found"));

        if (addressRepository
                .existsByDeliveryAreaIdAndDeletedAtIsNull(id)) {

            throw new ResourceConflictException(
                    "Cannot delete delivery area because it still has active addresses."
            );
        }

        deliveryArea.setDeletedAt(LocalDateTime.now());

        deliveryAreaRepository.save(deliveryArea);
    }

    private DeliveryAreaResponseDto toDto(
            DeliveryArea deliveryArea) {

        return new DeliveryAreaResponseDto(
                deliveryArea.getId(),
                deliveryArea.getCity().getId(),
                deliveryArea.getName()
        );
    }
}