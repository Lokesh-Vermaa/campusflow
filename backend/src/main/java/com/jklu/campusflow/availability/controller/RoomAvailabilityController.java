package com.jklu.campusflow.availability.controller;

import com.jklu.campusflow.availability.dto.AvailabilityResponse;
import com.jklu.campusflow.availability.dto.RoomScheduleResponse;
import com.jklu.campusflow.availability.service.RoomAvailabilityService;
import com.jklu.campusflow.common.error.BusinessRuleException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
public class RoomAvailabilityController {

    private final RoomAvailabilityService availabilityService;

    public RoomAvailabilityController(RoomAvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<AvailabilityResponse> searchAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(required = false) Integer minCapacity,
            @RequestParam(required = false) UUID blockId,
            @RequestParam(required = false) String features,
            org.springframework.security.core.Authentication auth
    ) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("endTime must be strictly after startTime");
        }

        String role = null;
        if (auth != null) {
            role = auth.getAuthorities().stream()
                    .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                    .map(a -> a.replace("ROLE_", ""))
                    .findFirst().orElse(null);
        }

        AvailabilityResponse response = availabilityService.searchAvailability(
                date, startTime, endTime, minCapacity, blockId, features, role
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<RoomScheduleResponse> getRoomSchedule(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        RoomScheduleResponse response = availabilityService.getRoomSchedule(id, targetDate);
        return ResponseEntity.ok(response);
    }
}