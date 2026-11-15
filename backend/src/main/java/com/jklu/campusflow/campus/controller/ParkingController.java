package com.jklu.campusflow.campus.controller;

import com.jklu.campusflow.campus.dto.ParkingPermitRequest;
import com.jklu.campusflow.campus.dto.ParkingPermitResponse;
import com.jklu.campusflow.campus.dto.ParkingZoneResponse;
import com.jklu.campusflow.campus.service.CampusDataService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final CampusDataService campusDataService;

    public ParkingController(CampusDataService campusDataService) {
        this.campusDataService = campusDataService;
    }

    @GetMapping("/zones")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<List<ParkingZoneResponse>> listZones() {
        return ResponseEntity.ok(campusDataService.listParkingZones());
    }

    @PostMapping("/permits")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<ParkingPermitResponse> requestPermit(
            @Valid @RequestBody ParkingPermitRequest request,
            Authentication auth
    ) {
        ParkingPermitResponse response = campusDataService.requestParkingPermit(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}