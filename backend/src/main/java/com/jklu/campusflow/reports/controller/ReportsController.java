package com.jklu.campusflow.reports.controller;

import com.jklu.campusflow.reports.dto.UtilisationReportResponse;
import com.jklu.campusflow.reports.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    private final ReportService reportService;

    public ReportsController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/utilisation")
    public ResponseEntity<UtilisationReportResponse> getUtilisation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) UUID blockId
    ) {
        return ResponseEntity.ok(reportService.getUtilisationReport(startDate, endDate, blockId));
    }
}
