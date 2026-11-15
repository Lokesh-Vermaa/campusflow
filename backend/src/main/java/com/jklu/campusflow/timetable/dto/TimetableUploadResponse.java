package com.jklu.campusflow.timetable.dto;

import java.util.List;
import java.util.UUID;

public record TimetableUploadResponse(
        UUID uploadId,
        int totalRows,
        int validRows,
        List<TimetableErrorRow> errorRows,
        List<TimetableRowParsed> preview
) {}