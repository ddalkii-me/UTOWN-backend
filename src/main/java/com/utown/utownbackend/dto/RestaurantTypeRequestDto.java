package com.utown.utownbackend.dto;


import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;

public record RestaurantTypeRequestDto(

        @NotBlank(message = "Restaurant type name is required")
        String name,

        String description
) { }