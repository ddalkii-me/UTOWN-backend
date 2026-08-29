package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.CityRequestDto;
import com.utown.utownbackend.dto.CityResponseDto;

import java.util.List;

public interface CityService {

    CityResponseDto createCity(CityRequestDto request);

    List<CityResponseDto> getAllCities();

    CityResponseDto getCityById(Long id);

    CityResponseDto updateCity(Long id, CityRequestDto request);

    void deleteCity(Long id);
}