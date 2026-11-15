package com.jklu.campusflow.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ParkingPermitRequest(
        @NotBlank String visitorName,
        @NotBlank String vehicleNumber,
        @NotBlank String vehicleType,
        @NotNull UUID zoneId,
        @NotNull LocalDate visitDate,
        @NotBlank String startTime,
        @NotBlank String endTime,
        @NotBlank String purpose
) {}