package com.jklu.campusflow.timetable.dto;

public record TimetablePublishResponse(
        String status,
        String semester,
        int publishedSlots,
        int publishedCount,
        String message
) {
    public TimetablePublishResponse(String status, String semester, int publishedSlots) {
        this(status, semester, publishedSlots, publishedSlots, "Successfully published " + publishedSlots + " timetable slots for " + semester);
    }
}