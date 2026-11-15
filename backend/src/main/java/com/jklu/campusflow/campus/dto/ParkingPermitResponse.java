package com.jklu.campusflow.campus.dto;

import java.util.UUID;

public record ParkingPermitResponse(
        UUID permitId,
        String permitCode,
        String status,
        String allocatedSlot,
        String validUntil
) {}