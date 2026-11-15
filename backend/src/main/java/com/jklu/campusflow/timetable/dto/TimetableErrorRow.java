package com.jklu.campusflow.timetable.dto;

public record TimetableErrorRow(
        int rowNumber,
        String course,
        String room,
        String error
) {}