package com.jklu.campusflow.campus.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record GuestRoomResponse(
        UUID id,
        String roomNumber,
        String roomType,
        String bedType,
        int capacity,
        boolean isAvailable,
        List<String> amenities,
        BigDecimal ratePerNight
) {}