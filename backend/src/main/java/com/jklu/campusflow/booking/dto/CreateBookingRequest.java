package com.jklu.campusflow.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        UUID parkingZoneId,
        String visitorName,
        String vehicleNumber,
        @Valid List<ParkingRequestItem> parkingRequests
) {
    public CreateBookingRequest {
        if ((parkingRequests == null || parkingRequests.isEmpty()) && parkingZoneId != null) {
            parkingRequests = List.of(new ParkingRequestItem(
                    visitorName != null && !visitorName.isBlank() ? visitorName : "Campus Visitor",
                    vehicleNumber != null && !vehicleNumber.isBlank() ? vehicleNumber : "N/A",
                    parkingZoneId
            ));
        }
    }

    public CreateBookingRequest(
            String title,
            String description,
            String resourceType,
            UUID resourceId,
            LocalDate bookingDate,
            String startTime,
            String endTime,
            Integer expectedAttendees,
            UUID guestHouseRoomId,
            LocalDate guestHouseCheckIn,
            LocalDate guestHouseCheckOut,
            List<ParkingRequestItem> parkingRequests
    ) {
        this(title, description, resourceType, resourceId, bookingDate, startTime, endTime, expectedAttendees,
             guestHouseRoomId, guestHouseCheckIn, guestHouseCheckOut, null, null, null, parkingRequests);
    }
}

