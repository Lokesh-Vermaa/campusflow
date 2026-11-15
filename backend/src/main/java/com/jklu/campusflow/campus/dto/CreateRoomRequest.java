package com.jklu.campusflow.campus.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateRoomRequest(
        @NotNull(message = "Block ID is required") UUID blockId,
        @NotBlank(message = "Room number is required") @Size(max = 50, message = "Room number cannot exceed 50 characters") String roomNumber,
        @Size(max = 100, message = "Name cannot exceed 100 characters") String name,
        @NotBlank(message = "Room type is required")
        @Pattern(regexp = "CLASSROOM|LAB|AMPHITHEATRE", message = "Room type must be CLASSROOM, LAB, or AMPHITHEATRE")
        String type,
        @Min(value = 1, message = "Capacity must be greater than 0")
        int capacity,
        List<String> features,
        Boolean active,
        Boolean bookableOutside
) {}
