package com.utown.utownbackend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequestDto(

        @NotNull
        @Min(1)
        @Max(5)
        Integer score,

        String comment
) {
}