package com.jklu.campusflow.availability.dto;

import java.util.List;
import java.util.UUID;

public record UnavailableRoomDto(
        UUID id,
        String roomNumber,
        String block,
        int floor,
        int capacity,
        String type,
        List<String> features,
        String conflictReason,
        List<TimelineSlotDto> timeline
) {}