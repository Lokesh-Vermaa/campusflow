package com.jklu.campusflow.booking.service;

import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.booking.dto.BookingResponse;
import com.jklu.campusflow.booking.dto.CreateBookingRequest;
import com.jklu.campusflow.booking.dto.ParkingRequestItem;
import com.jklu.campusflow.booking.model.Approval;
import com.jklu.campusflow.booking.model.Booking;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.booking.repository.ApprovalRepository;
import com.jklu.campusflow.booking.repository.BookingItemRepository;
import com.jklu.campusflow.booking.repository.BookingRepository;
import com.jklu.campusflow.campus.model.GuestRoom;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.GuestRoomRepository;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.BusinessRuleException;
import com.jklu.campusflow.common.error.ConflictException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class BookingService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ApprovalRepository approvalRepository;
    private final RoomRepository roomRepository;
    private final GuestRoomRepository guestRoomRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            ApprovalRepository approvalRepository,
            RoomRepository roomRepository,
            GuestRoomRepository guestRoomRepository,
            ParkingZoneRepository parkingZoneRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.approvalRepository = approvalRepository;
        this.roomRepository = roomRepository;
        this.guestRoomRepository = guestRoomRepository;
        this.parkingZoneRepository = parkingZoneRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public BookingResponse createBooking(CreateBookingRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        LocalTime startTime = LocalTime.parse(request.startTime());
        LocalTime endTime = LocalTime.parse(request.endTime());
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("endTime must be strictly after startTime");
        }

        Room room = roomRepository.findActiveByIdWithBlock(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.resourceId()));

        if (!room.isBookableOutside()) {
            throw new BusinessRuleException("Room " + room.getRoomNumber() + " is restricted and not bookable for outside events.");
        }

        // Role restriction: STUDENT can only book CLASSROOMs
        if ("STUDENT".equalsIgnoreCase(user.getRole()) && !"CLASSROOM".equalsIgnoreCase(room.getType())) {
            throw new AccessDeniedException("Students are only permitted to book standard CLASSROOM venues.");
        }

        OffsetDateTime startTz = request.bookingDate().atTime(startTime).atZone(ZONE).toOffsetDateTime();
        OffsetDateTime endTz = request.bookingDate().atTime(endTime).atZone(ZONE).toOffsetDateTime();

        String year = String.valueOf(LocalDate.now().getYear());
        int randomSeq = ThreadLocalRandom.current().nextInt(1000, 9999);
        String bookingNumber = "JKLU-BK-" + year + "-" + randomSeq;

        Booking booking = new Booking(
                null,
                bookingNumber,
                user,
                request.title(),
                request.description(),
                "PENDING_APPROVAL",
                "Campus Event",
                request.expectedAttendees() != null ? request.expectedAttendees() : 1
        );
        booking = bookingRepository.save(booking);

        List<BookingItem> items = new ArrayList<>();
        // 1. Primary room item
        items.add(new BookingItem(booking.getId(), "ROOM", room.getId(), startTz, endTz, "PENDING_APPROVAL"));

        // 2. Bundle item: Guest Room (all-or-nothing)
        if (request.guestHouseRoomId() != null) {
            GuestRoom gr = guestRoomRepository.findById(request.guestHouseRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Guest room not found: " + request.guestHouseRoomId()));

            LocalDate checkIn = request.guestHouseCheckIn() != null ? request.guestHouseCheckIn() : request.bookingDate();
            LocalDate checkOut = request.guestHouseCheckOut() != null ? request.guestHouseCheckOut() : request.bookingDate().plusDays(1);
            if (!checkOut.isAfter(checkIn)) {
                throw new BusinessRuleException("guestHouseCheckOut must be strictly after guestHouseCheckIn");
            }

            OffsetDateTime grStart = checkIn.atTime(12, 0).atZone(ZONE).toOffsetDateTime();
            OffsetDateTime grEnd = checkOut.atTime(11, 0).atZone(ZONE).toOffsetDateTime();
            items.add(new BookingItem(booking.getId(), "GUEST_ROOM", gr.getId(), grStart, grEnd, "PENDING_APPROVAL"));
        }

        // 3. Bundle items: Parking permits (all-or-nothing)
        if (request.parkingRequests() != null) {
            for (ParkingRequestItem p : request.parkingRequests()) {
                ParkingZone zone = parkingZoneRepository.findByIdForUpdate(p.zoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parking zone not found: " + p.zoneId()));
                if (zone.getTotalSlots() <= 0) {
                    throw new ConflictException("Parking zone " + zone.getName() + " is currently full.");
                }
                items.add(new BookingItem(booking.getId(), "PARKING", zone.getId(), startTz, endTz, "PENDING_APPROVAL"));
            }
        }

        try {
            bookingItemRepository.saveAllAndFlush(items);
        } catch (DataIntegrityViolationException e) {
            // PostgreSQL exclusion constraint violation (ex_booking_item_no_overlap)
            throw new ConflictException("The selected slot for room " + room.getRoomNumber() + " is already booked or held by a pending reservation.");
        }

        auditService.record(
                user.getId(),
                "BOOKING_CREATED",
                "BOOKING",
                booking.getId().toString(),
                null,
                "{\"bookingNumber\":\"" + bookingNumber + "\",\"status\":\"PENDING\"}",
                "127.0.0.1",
                "Internal/API"
        );

        return toResponse(booking, room.getName() != null ? room.getName() : room.getRoomNumber(), request.bookingDate(), request.startTime(), request.endTime());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookingResponse> listMyBookings(UUID userId, String statusFilter, int page, int pageSize) {
        int safePage = Math.max(0, page - 1);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        Pageable pageable = PageRequest.of(safePage, safePageSize);

        String dbStatus = mapContractStatusToDb(statusFilter);
        Page<Booking> pageResult;
        if (dbStatus != null) {
            pageResult = bookingRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, dbStatus, pageable);
        } else {
            pageResult = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<BookingResponse> items = pageResult.getContent().stream()
                .map(this::toResponseSummary)
                .toList();

        return new PaginatedResponse<>(items, page, safePageSize, pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    public BookingResponse cancelBooking(UUID bookingId, UUID currentUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        // IDOR Check
        if (!isAdmin && !booking.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to cancel another user's booking.");
        }

        if ("CANCELLED".equals(booking.getStatus()) || "REJECTED".equals(booking.getStatus())) {
            throw new BusinessRuleException("Booking is already in " + booking.getStatus() + " state.");
        }

        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(OffsetDateTime.now());
        bookingRepository.save(booking);

        // Release all held slots
        List<BookingItem> bookingItems = booking.getItems();
        for (BookingItem item : bookingItems) {
            item.setStatus("CANCELLED");
        }
        bookingItemRepository.saveAll(bookingItems);

        auditService.record(
                currentUserId,
                "BOOKING_CANCELLED",
                "BOOKING",
                booking.getId().toString(),
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"CANCELLED\"}",
                "127.0.0.1",
                "Internal/API"
        );

        return toResponseSummary(booking);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookingResponse> getPendingApprovals(int page, int pageSize) {
        int safePage = Math.max(0, page - 1);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        Pageable pageable = PageRequest.of(safePage, safePageSize);

        Page<Booking> pageResult = bookingRepository.findPendingApprovals(pageable);
        List<BookingResponse> items = pageResult.getContent().stream()
                .map(this::toResponseSummary)
                .toList();

        return new PaginatedResponse<>(items, page, safePageSize, pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    public BookingResponse approveBooking(UUID bookingId, UUID adminUserId, String note) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!"PENDING_APPROVAL".equals(booking.getStatus())) {
            throw new BusinessRuleException("Only PENDING bookings can be approved. Current status: " + booking.getStatus());
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        booking.setStatus("CONFIRMED");
        booking.setUpdatedAt(OffsetDateTime.now());
        bookingRepository.save(booking);

        for (BookingItem bi : booking.getItems()) {
            bi.setStatus("CONFIRMED");
        }
        bookingItemRepository.saveAll(booking.getItems());

        approvalRepository.save(new Approval(booking.getId(), admin, "APPROVED", note));

        auditService.record(
                adminUserId,
                "APPROVAL",
                "BOOKING",
                booking.getId().toString(),
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"APPROVED\",\"note\":\"" + (note != null ? note : "") + "\"}",
                "127.0.0.1",
                "Internal/API"
        );

        return toResponseSummary(booking);
    }

    public BookingResponse rejectBooking(UUID bookingId, UUID adminUserId, String reason) {
        if (reason == null || reason.trim().length() < 3) {
            throw new BusinessRuleException("A valid rejection reason (minimum 3 characters) is required.");
        }

        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!"PENDING_APPROVAL".equals(booking.getStatus())) {
            throw new BusinessRuleException("Only PENDING bookings can be rejected. Current status: " + booking.getStatus());
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        booking.setStatus("REJECTED");
        booking.setUpdatedAt(OffsetDateTime.now());
        bookingRepository.save(booking);

        // Rejection releases the slots so other users can book them
        for (BookingItem bi : booking.getItems()) {
            bi.setStatus("REJECTED");
            bi.setRejectionReason(reason);
        }
        bookingItemRepository.saveAll(booking.getItems());

        approvalRepository.save(new Approval(booking.getId(), admin, "REJECTED", reason));

        auditService.record(
                adminUserId,
                "REJECTION",
                "BOOKING",
                booking.getId().toString(),
                "{\"status\":\"PENDING\"}",
                "{\"status\":\"REJECTED\",\"reason\":\"" + reason + "\"}",
                "127.0.0.1",
                "Internal/API"
        );

        return toResponseSummary(booking);
    }

    private BookingResponse toResponse(Booking b, String resourceName, LocalDate bookingDate, String startTime, String endTime) {
        return new BookingResponse(
                b.getId(),
                b.getBookingNumber(),
                b.getTitle(),
                mapDbStatusToContract(b.getStatus()),
                b.getUser().getId(),
                b.getUser().getFullName(),
                resourceName,
                bookingDate,
                startTime,
                endTime,
                b.getCreatedAt()
        );
    }

    private BookingResponse toResponseSummary(Booking b) {
        LocalDate date = b.getCreatedAt().toLocalDate();
        String startTime = "09:00";
        String endTime = "18:00";
        String resName = "Campus Venue";

        if (b.getItems() != null && !b.getItems().isEmpty()) {
            BookingItem first = b.getItems().get(0);
            date = first.getStartTime().toLocalDate();
            startTime = first.getStartTime().toLocalTime().toString();
            endTime = first.getEndTime().toLocalTime().toString();
        }

        return new BookingResponse(
                b.getId(),
                b.getBookingNumber(),
                b.getTitle(),
                mapDbStatusToContract(b.getStatus()),
                b.getUser().getId(),
                b.getUser().getFullName(),
                resName,
                date,
                startTime,
                endTime,
                b.getCreatedAt()
        );
    }

    private String mapContractStatusToDb(String status) {
        if (status == null || status.isBlank()) return null;
        return switch (status.toUpperCase()) {
            case "PENDING" -> "PENDING_APPROVAL";
            case "APPROVED" -> "CONFIRMED";
            case "REJECTED" -> "REJECTED";
            case "CANCELLED" -> "CANCELLED";
            default -> status.toUpperCase();
        };
    }

    private String mapDbStatusToContract(String dbStatus) {
        if (dbStatus == null) return "PENDING";
        return switch (dbStatus) {
            case "PENDING_APPROVAL" -> "PENDING";
            case "CONFIRMED" -> "APPROVED";
            default -> dbStatus;
        };
    }
}
