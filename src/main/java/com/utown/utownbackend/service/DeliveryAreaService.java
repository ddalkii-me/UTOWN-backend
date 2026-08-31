package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DeliveryAreaRequestDto;
import com.utown.utownbackend.dto.DeliveryAreaResponseDto;

import java.util.List;

public interface DeliveryAreaService {

    DeliveryAreaResponseDto createDeliveryArea(
            DeliveryAreaRequestDto request
    );

    List<DeliveryAreaResponseDto> getAllDeliveryAreas();

    List<DeliveryAreaResponseDto> getDeliveryAreasByCity(
            Long cityId
    );

    DeliveryAreaResponseDto getDeliveryAreaById(
            Long id
    );

    DeliveryAreaResponseDto updateDeliveryArea(
            Long id,
            DeliveryAreaRequestDto request
    );

    void deleteDeliveryArea(
            Long id
    );
}