package com.jklu.campusflow.availability.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RoomScheduleResponse(
        UUID roomId,
        String roomNumber,
        LocalDate date,
        List<ScheduleSlotDto> slots
) {}