package com.jklu.campusflow.reports.dto;

import java.util.UUID;

public record RoomUtilisationDto(
        UUID roomId,
        String roomNumber,
        String name,
        long bookingCount,
        double utilPct
) {}
