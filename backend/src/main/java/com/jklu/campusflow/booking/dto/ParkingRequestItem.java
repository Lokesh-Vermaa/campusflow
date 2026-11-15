package com.jklu.campusflow.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ParkingRequestItem(
        @NotBlank(message = "Visitor name is required") String visitorName,
        @NotBlank(message = "Vehicle number is required") String vehicleNumber,
        @NotNull(message = "Parking zone ID is required") UUID zoneId
) {}
