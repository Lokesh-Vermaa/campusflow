package com.jklu.campusflow.common.error;

import com.jklu.campusflow.booking.dto.ConflictWithAlternativesResponse.AlternativeRoom;
import java.util.List;

public class BookingConflictException extends RuntimeException {
    private final List<AlternativeRoom> alternatives;
    private final List<String> nearbySlots;

    public BookingConflictException(String message, List<AlternativeRoom> alternatives, List<String> nearbySlots) {
        super(message);
        this.alternatives = alternatives != null ? alternatives : List.of();
        this.nearbySlots = nearbySlots != null ? nearbySlots : List.of();
    }

    public List<AlternativeRoom> getAlternatives() {
        return alternatives;
    }

    public List<String> getNearbySlots() {
        return nearbySlots;
    }
}
