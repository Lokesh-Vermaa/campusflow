package com.jklu.campusflow.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotBlank(message = "Title is required") @Size(max = 255, message = "Title cannot exceed 255 characters") String title,
        String description,
        @NotBlank(message = "Resource type is required") String resourceType,
        @NotNull(message = "Resource ID is required") UUID resourceId,
        @NotNull(message = "Booking date is required") LocalDate bookingDate,
        @NotBlank(message = "Start time is required") String startTime,
        @NotBlank(message = "End time is required") String endTime,
        Integer expectedAttendees,
        UUID guestHouseRoomId,
        LocalDate guestHouseCheckIn,
        LocalDate guestHouseCheckOut,
        @Valid List<ParkingRequestItem> parkingRequests
) {}
