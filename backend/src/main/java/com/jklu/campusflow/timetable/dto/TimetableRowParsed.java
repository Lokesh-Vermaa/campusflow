package com.jklu.campusflow.timetable.dto;

public record TimetableRowParsed(
        int rowNumber,
        String course,
        String roomNumber,
        int dayOfWeek,
        String startTime,
        String endTime,
        int startMinute,
        int endMinute,
        String section,
        String faculty,
        String academicTerm,
        boolean isValid,
        String error
) {}