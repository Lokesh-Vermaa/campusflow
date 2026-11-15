package com.jklu.campusflow.availability.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailabilityResponse(
        LocalDate date,
        TimeIntervalDto requestedInterval,
        List<AvailableRoomDto> availableRooms,
        List<UnavailableRoomDto> unavailableRooms
) {}