package com.jklu.campusflow.availability.dto;

public record ScheduleSlotDto(
        String startTime,
        String endTime,
        String status,
        String title,
        String category
) {}