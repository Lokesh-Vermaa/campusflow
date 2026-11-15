package com.jklu.campusflow.booking.controller;

import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.booking.dto.ApprovalDecisionRequest;
import com.jklu.campusflow.booking.dto.BookingResponse;
import com.jklu.campusflow.booking.service.BookingService;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/approvals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public AdminApprovalController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<BookingResponse>> getApprovalQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        PaginatedResponse<BookingResponse> response = bookingService.getPendingApprovals(page, pageSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveBooking(
            @PathVariable UUID id,
            @RequestBody(required = false) ApprovalDecisionRequest request,
            Authentication auth
    ) {
        User admin = resolveUser(auth);
        String note = request != null ? request.note() : "Approved by administrator";
        BookingResponse response = bookingService.approveBooking(id, admin.getId(), note);
        return ResponseEntity.ok(Map.of(
                "id", response.id(),
                "status", "APPROVED",
                "message", "Booking approved successfully"
        ));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectBooking(
            @PathVariable UUID id,
            @RequestBody ApprovalDecisionRequest request,
            Authentication auth
    ) {
        User admin = resolveUser(auth);
        String reason = request != null ? request.reason() : null;
        BookingResponse response = bookingService.rejectBooking(id, admin.getId(), reason);
        return ResponseEntity.ok(Map.of(
                "id", response.id(),
                "status", "REJECTED",
                "message", "Booking rejected: " + reason
        ));
    }

    private User resolveUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated admin user not found: " + email));
    }
}
