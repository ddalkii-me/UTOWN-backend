package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record CityRequestDto(

        @NotBlank(message = "City name is required")
        String name

) {
}