package com.jklu.campusflow.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.booking.dto.BookingResponse;
import com.jklu.campusflow.booking.dto.CreateBookingRequest;
import com.jklu.campusflow.booking.service.BookingService;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import com.jklu.campusflow.common.idempotency.IdempotencyKey;
import com.jklu.campusflow.common.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public BookingController(
            BookingService bookingService,
            UserRepository userRepository,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper
    ) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<?> createBooking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBookingRequest request,
            Authentication auth
    ) throws Exception {
        User user = resolveUser(auth);
        String payloadJson = objectMapper.writeValueAsString(request);

        // 1. Idempotency replay check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> cached = idempotencyService.findValidKey(idempotencyKey, user.getId(), payloadJson);
            if (cached.isPresent()) {
                BookingResponse cachedBody = objectMapper.readValue(cached.get().getResponseBody(), BookingResponse.class);
                return ResponseEntity.status(cached.get().getResponseCode()).body(cachedBody);
            }
        }

        // 2. Execute booking creation
        BookingResponse response = bookingService.createBooking(request, user.getId());

        // 3. Save idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveKey(idempotencyKey, user.getId(), payloadJson, HttpStatus.CREATED.value(), objectMapper.writeValueAsString(response));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<PaginatedResponse<BookingResponse>> getMyBookings(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication auth
    ) {
        User user = resolveUser(auth);
        PaginatedResponse<BookingResponse> response = bookingService.listMyBookings(user.getId(), status, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable UUID id,
            Authentication auth
    ) {
        User user = resolveUser(auth);
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        BookingResponse response = bookingService.cancelBooking(id, user.getId(), isAdmin);
        return ResponseEntity.ok(Map.of(
                "id", response.id(),
                "status", "CANCELLED",
                "message", "Booking cancelled successfully"
        ));
    }

    private User resolveUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + email));
    }
}
