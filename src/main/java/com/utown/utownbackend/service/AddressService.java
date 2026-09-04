package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.AddressRequestDto;
import com.utown.utownbackend.dto.AddressResponseDto;

import java.util.List;

public interface AddressService {

    AddressResponseDto createAddress(AddressRequestDto request);

    List<AddressResponseDto> getAllAddresses();

    List<AddressResponseDto> getAddressesByUser(Long userId);

    AddressResponseDto getAddressById(Long id);

    AddressResponseDto updateAddress(Long id, AddressRequestDto request);

    void deleteAddress(Long id);
}