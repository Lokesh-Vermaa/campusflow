package com.jklu.campusflow.campus.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ParkingZoneResponse(
        UUID id,
        String name,
        String code,
        int totalSlots,
        int occupiedSlots,
        int availableSlots,
        String status,
        BigDecimal ratePerHour
) {}