package com.jklu.campusflow.campus.controller;

import com.jklu.campusflow.campus.dto.VenueResponse;
import com.jklu.campusflow.campus.service.CampusDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final CampusDataService campusDataService;

    public VenueController(CampusDataService campusDataService) {
        this.campusDataService = campusDataService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<List<VenueResponse>> listVenues(Authentication auth) {
        return ResponseEntity.ok(campusDataService.listVenues(auth));
    }
}