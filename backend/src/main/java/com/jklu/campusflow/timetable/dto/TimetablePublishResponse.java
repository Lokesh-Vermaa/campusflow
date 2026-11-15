package com.jklu.campusflow.timetable.dto;

public record TimetablePublishResponse(
        String status,
        String semester,
        int publishedSlots
) {}