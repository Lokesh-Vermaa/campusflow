package com.jklu.campusflow.booking.dto;

import com.jklu.campusflow.availability.dto.AvailableRoomDto;
import java.util.List;

public record ConflictWithAlternativesResponse(
        String code,
        String message,
        String requestId,
        List<AlternativeRoom> alternatives,
        List<String> nearbySlots
) {
    public record AlternativeRoom(String roomId, String roomNumber, int capacity, String type, List<String> freeWindows) {}
}
