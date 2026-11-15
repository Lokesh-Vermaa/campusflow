package com.jklu.campusflow.booking.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String bookingNumber,
        String title,
        String status,
        UUID requesterId,
        String requesterName,
        String resourceName,
        LocalDate bookingDate,
        String startTime,
        String endTime,
        OffsetDateTime createdAt
) {}
