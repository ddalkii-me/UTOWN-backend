package com.utown.utownbackend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record WorkingHoursDto(@NotNull DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean dayOff) {
}
