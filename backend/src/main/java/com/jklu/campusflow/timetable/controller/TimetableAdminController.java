package com.jklu.campusflow.timetable.controller;

import com.jklu.campusflow.timetable.dto.TimetablePublishRequest;
import com.jklu.campusflow.timetable.dto.TimetablePublishResponse;
import com.jklu.campusflow.timetable.dto.TimetableUploadResponse;
import com.jklu.campusflow.timetable.service.TimetableService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/timetable")
@PreAuthorize("hasRole('ADMIN')")
public class TimetableAdminController {

    private final TimetableService timetableService;

    public TimetableAdminController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @PostMapping("/upload")
    public ResponseEntity<TimetableUploadResponse> uploadTimetable(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "academicTerm", required = false) String academicTerm,
            Authentication auth
    ) {
        UUID userId = null; // Stored if user is identified
        TimetableUploadResponse response = timetableService.uploadAndValidate(file, academicTerm, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/publish")
    public ResponseEntity<TimetablePublishResponse> publishTimetable(
            @RequestBody TimetablePublishRequest request
    ) {
        TimetablePublishResponse response = timetableService.publishTimetable(request);
        return ResponseEntity.ok(response);
    }
}