package com.jklu.campusflow.timetable.dto;

import java.util.List;
import java.util.UUID;

public record TimetablePublishRequest(
        String semester,
        UUID uploadId,
        List<TimetableRowParsed> entries
) {}