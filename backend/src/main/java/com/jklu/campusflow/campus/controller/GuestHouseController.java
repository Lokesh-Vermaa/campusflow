package com.jklu.campusflow.campus.controller;

import com.jklu.campusflow.campus.dto.GuestRoomResponse;
import com.jklu.campusflow.campus.service.CampusDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guesthouse")
public class GuestHouseController {

    private final CampusDataService campusDataService;

    public GuestHouseController(CampusDataService campusDataService) {
        this.campusDataService = campusDataService;
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<GuestRoomResponse>> listRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut
    ) {
        return ResponseEntity.ok(campusDataService.listGuestRooms(checkIn, checkOut));
    }
}