package com.jklu.campusflow.availability.service;

import com.jklu.campusflow.availability.dto.AvailabilityResponse;
import com.jklu.campusflow.availability.dto.RoomScheduleResponse;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.campus.model.Block;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.timetable.model.TimetableSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for RoomAvailabilityEngine — no Spring context required.
 * All inputs are constructed in-memory; zero DB queries.
 */
class RoomAvailabilityEngineTest {

    private RoomAvailabilityEngine engine;
    private Block blockA;

    @BeforeEach
    void setUp() {
        engine = new RoomAvailabilityEngine();
        blockA = new Block(UUID.randomUUID(), "Block A", "BLK-A", true);
    }

    // -----------------------------------------------------------------------
    // Helper factories
    // -----------------------------------------------------------------------

    private Room makeRoom(String roomNumber, String type, int capacity, List<String> features) {
        return new Room(UUID.randomUUID(), blockA, roomNumber, roomNumber, type, capacity, features, true, true);
    }

    private TimetableSlot makeSlot(Room room, int dayOfWeek, int startMin, int endMin) {
        TimetableSlot ts = new TimetableSlot(room, dayOfWeek, startMin, endMin, "CS101", "A", "Dr. Test", "Spring 2027");
        return ts;
    }

    private BookingItem makeBooking(UUID resourceId, int startHour, int endHour) {
        OffsetDateTime start = LocalDate.of(2026, 11, 12)
                .atTime(LocalTime.of(startHour, 0))
                .atOffset(ZoneOffset.ofHoursMinutes(5, 30));
        OffsetDateTime end = LocalDate.of(2026, 11, 12)
                .atTime(LocalTime.of(endHour, 0))
                .atOffset(ZoneOffset.ofHoursMinutes(5, 30));
        return new BookingItem(UUID.randomUUID(), "ROOM", resourceId, start, end, "CONFIRMED");
    }

    // -----------------------------------------------------------------------
    // Test 1: Touching intervals do NOT overlap (10:00–11:00 and 11:00–12:00)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Touching intervals [10:00-11:00] and [11:00-12:00] must NOT overlap")
    void touchingIntervalsMustNotOverlap() {
        // 10:00 = 600 min, 11:00 = 660 min, 12:00 = 720 min
        boolean overlap = RoomAvailabilityEngine.intervalsOverlap(600, 660, 660, 720);
        assertThat(overlap).isFalse();
    }

    // -----------------------------------------------------------------------
    // Test 2: Room with no conflicts is available for a free slot
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Room with no timetable or booking conflicts appears in availableRooms")
    void roomWithNoConflictIsAvailable() {
        Room room = makeRoom("A-101", "CLASSROOM", 60, List.of("projector"));
        // Date is Wednesday (3); timetable slot is on Monday (1) — no conflict
        TimetableSlot slot = makeSlot(room, 1 /* Monday */, 540, 600); // 09:00-10:00

        // Request is Wednesday 14:00-16:00
        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(room),
                List.of(slot),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 11), // Wednesday
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                null, null, null
        );

        assertThat(resp.availableRooms()).hasSize(1);
        assertThat(resp.unavailableRooms()).isEmpty();
        assertThat(resp.availableRooms().get(0).roomNumber()).isEqualTo("A-101");
    }

    // -----------------------------------------------------------------------
    // Test 3: Booking conflict marks room unavailable
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Active booking in the same window marks room as unavailable")
    void bookingConflictMarksUnavailable() {
        Room room = makeRoom("A-102", "CLASSROOM", 60, Collections.emptyList());
        // Booking 14:00-16:00 on the same day
        BookingItem booking = makeBooking(room.getId(), 14, 16);

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(room),
                Collections.emptyList(),
                List.of(booking),
                LocalDate.of(2026, 11, 12),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                null, null, null
        );

        assertThat(resp.availableRooms()).isEmpty();
        assertThat(resp.unavailableRooms()).hasSize(1);
        assertThat(resp.unavailableRooms().get(0).roomNumber()).isEqualTo("A-102");
    }

    // -----------------------------------------------------------------------
    // Test 4: minCapacity filter removes under-capacity rooms from available set
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("minCapacity filter removes rooms whose capacity is below threshold")
    void minCapacityFilterExcludesSmallRooms() {
        Room small = makeRoom("A-101", "CLASSROOM", 20, Collections.emptyList());
        Room large = makeRoom("A-201", "CLASSROOM", 80, Collections.emptyList());

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(small, large),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                30, null, null   // minCapacity = 30
        );

        assertThat(resp.availableRooms()).hasSize(1);
        assertThat(resp.availableRooms().get(0).roomNumber()).isEqualTo("A-201");
    }

    // -----------------------------------------------------------------------
    // Test 5: Schedule heatmap fills day correctly with FREE slots
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("computeSchedule fills full day heatmap; free windows appear between busy slots")
    void scheduleHeatmapFillsDay() {
        Room room = makeRoom("T-101", "AMPHITHEATRE", 300, List.of("projector"));
        // Timetable: 09:00-10:00 on Monday (day 1)
        TimetableSlot slot = makeSlot(room, 1, 540, 600);

        RoomScheduleResponse resp = engine.computeSchedule(
                room,
                List.of(slot),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 9) // Monday
        );

        assertThat(resp.slots()).isNotEmpty();
        // First slot should be FREE from 08:00 to 09:00
        var firstSlot = resp.slots().get(0);
        assertThat(firstSlot.status()).isEqualTo("FREE");
        assertThat(firstSlot.startTime()).isEqualTo("08:00");
        assertThat(firstSlot.endTime()).isEqualTo("09:00");
        // Second slot should be BUSY 09:00-10:00
        var busySlot = resp.slots().get(1);
        assertThat(busySlot.status()).isEqualTo("BUSY");
        assertThat(busySlot.startTime()).isEqualTo("09:00");
        assertThat(busySlot.endTime()).isEqualTo("10:00");
    }

    // -----------------------------------------------------------------------
    // B3 Test 6: Inactive rooms are excluded from availability search
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("B3: Inactive rooms are excluded from availability search")
    void testInactiveRoomIsExcluded() {
        Room activeRoom = makeRoom("A-101", "CLASSROOM", 50, List.of("projector"));
        Room inactiveRoom = makeRoom("A-102", "CLASSROOM", 50, List.of("projector"));
        inactiveRoom.setActive(false);

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(activeRoom, inactiveRoom),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 16),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null, null, null
        );

        assertThat(resp.availableRooms()).hasSize(1);
        assertThat(resp.availableRooms().get(0).roomNumber()).isEqualTo("A-101");
        assertThat(resp.unavailableRooms()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // B3 Test 7: Non-bookable-outside rooms are excluded from availability search
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("B3: Rooms not bookable outside are excluded from external availability search")
    void testNonBookableOutsideRoomIsExcluded() {
        Room bookable = makeRoom("A-101", "CLASSROOM", 50, List.of("projector"));
        Room notBookable = makeRoom("A-102", "CLASSROOM", 50, List.of("projector"));
        notBookable.setBookableOutside(false);

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(bookable, notBookable),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 16),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null, null, null
        );

        assertThat(resp.availableRooms()).hasSize(1);
        assertThat(resp.availableRooms().get(0).roomNumber()).isEqualTo("A-101");
    }

    // -----------------------------------------------------------------------
    // B3 Test 8: Role filtering - STUDENT sees only classrooms
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("B3: Role filtering ensures STUDENT sees only CLASSROOM rooms (labs/amphitheatres excluded)")
    void testRoleFilteringStudentSeesOnlyClassrooms() {
        Room classroom = makeRoom("A-101", "CLASSROOM", 50, Collections.emptyList());
        Room lab = makeRoom("T-101", "LAB", 40, Collections.emptyList());
        Room amphi = makeRoom("T-201", "AMPHITHEATRE", 120, Collections.emptyList());

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(classroom, lab, amphi),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 16),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null, null, null,
                "STUDENT"
        );

        assertThat(resp.availableRooms()).hasSize(1);
        assertThat(resp.availableRooms().get(0).roomNumber()).isEqualTo("A-101");
    }

    // -----------------------------------------------------------------------
    // B3 Test 9: Role filtering - STAFF sees classrooms, labs, and amphitheatres
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("B3: Role filtering allows STAFF to see classrooms, labs, and amphitheatres")
    void testRoleFilteringStaffSeesLabsAndAmphitheatres() {
        Room classroom = makeRoom("A-101", "CLASSROOM", 50, Collections.emptyList());
        Room lab = makeRoom("T-101", "LAB", 40, Collections.emptyList());
        Room amphi = makeRoom("T-201", "AMPHITHEATRE", 120, Collections.emptyList());

        AvailabilityResponse resp = engine.evaluateAvailability(
                List.of(classroom, lab, amphi),
                Collections.emptyList(),
                Collections.emptyList(),
                LocalDate.of(2026, 11, 16),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null, null, null,
                "STAFF"
        );

        assertThat(resp.availableRooms()).hasSize(3);
    }

    // -----------------------------------------------------------------------
    // B3 Test 10: Asia/Kolkata timezone edges and booking boundary
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("B3: Asia/Kolkata timezone offset is correctly evaluated for booking time window")
    void testAsiaKolkataTimezoneEdgeIntervals() {
        Room room = makeRoom("A-101", "CLASSROOM", 60, Collections.emptyList());
        // Booking strictly 09:00 to 10:00 IST
        BookingItem booking = makeBooking(room.getId(), 9, 10);

        // Request 10:00 to 11:00 IST (touching interval) -> Available!
        AvailabilityResponse respTouching = engine.evaluateAvailability(
                List.of(room),
                Collections.emptyList(),
                List.of(booking),
                LocalDate.of(2026, 11, 12),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null, null, null
        );
        assertThat(respTouching.availableRooms()).hasSize(1);

        // Request 09:30 to 10:30 IST (overlapping interval) -> Unavailable!
        AvailabilityResponse respOverlap = engine.evaluateAvailability(
                List.of(room),
                Collections.emptyList(),
                List.of(booking),
                LocalDate.of(2026, 11, 12),
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                null, null, null
        );
        assertThat(respOverlap.availableRooms()).isEmpty();
        assertThat(respOverlap.unavailableRooms()).hasSize(1);
    }
}
