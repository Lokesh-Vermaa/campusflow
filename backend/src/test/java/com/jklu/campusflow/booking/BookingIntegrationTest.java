package com.jklu.campusflow.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.booking.dto.ApprovalDecisionRequest;
import com.jklu.campusflow.booking.dto.CreateBookingRequest;
import com.jklu.campusflow.booking.dto.ParkingRequestItem;
import com.jklu.campusflow.booking.repository.BookingRepository;
import com.jklu.campusflow.campus.model.GuestRoom;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.GuestRoomRepository;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class BookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GuestRoomRepository guestRoomRepository;

    @Autowired
    private ParkingZoneRepository parkingZoneRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("Single booking creation returns 201 Created with status PENDING")
    void testCreateSingleBooking() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        CreateBookingRequest request = new CreateBookingRequest(
                "AI Workshop Sprint",
                "Hands-on workshop for ML beginners",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(20),
                "14:00",
                "16:00",
                30,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.title").value("AI Workshop Sprint"));
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("Idempotency-Key: replay with same payload returns cached 201; mismatch returns 400")
    void testIdempotencyKeyReplayAndMismatch() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        String idempotencyKey = UUID.randomUUID().toString();
        CreateBookingRequest request = new CreateBookingRequest(
                "Robotics Meetup",
                "Weekly robotics discussion",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(25),
                "10:00",
                "12:00",
                25,
                null,
                null,
                null,
                null
        );

        // 1. Initial creation
        MvcResult firstResult = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String firstBookingId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Exact replay with same key & same payload -> returns identical response
        MvcResult replayResult = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String replayBookingId = objectMapper.readTree(replayResult.getResponse().getContentAsString()).get("id").asText();
        assertThat(replayBookingId).isEqualTo(firstBookingId);

        // 3. Same key with different payload -> rejected with 422 Unprocessable Entity
        CreateBookingRequest differentPayload = new CreateBookingRequest(
                "Modified Title Meeting",
                "Altered description",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(26),
                "10:00",
                "12:00",
                25,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(differentPayload)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("Bundle booking (Room + Guest Room + Parking) all-or-nothing rollback on invalid resource")
    void testBundleBookingRollback() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(Room::isBookableOutside)
                .findFirst().orElseThrow();

        GuestRoom guestRoom = guestRoomRepository.findAll().get(0);
        ParkingZone parkingZone = parkingZoneRepository.findAll().get(0);

        long initialBookingCount = bookingRepository.count();

        // 1. Successful bundle booking
        CreateBookingRequest bundleRequest = new CreateBookingRequest(
                "National AI Conference",
                "Keynote address and guest reception",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(30),
                "09:00",
                "17:00",
                80,
                guestRoom.getId(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(32),
                List.of(new ParkingRequestItem("Prof. VIP Guest", "RJ-14-CA-1234", parkingZone.getId()))
        );

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bundleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(bookingRepository.count()).isEqualTo(initialBookingCount + 1);

        // 2. Failed bundle booking: non-existent guest house ID must rollback entire booking
        CreateBookingRequest failedBundleRequest = new CreateBookingRequest(
                "Corrupted Conference Request",
                "Invalid guest room bundle",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(35),
                "09:00",
                "17:00",
                80,
                UUID.randomUUID(), // non-existent guest room
                LocalDate.now().plusDays(35),
                LocalDate.now().plusDays(36),
                null
        );

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failedBundleRequest)))
                .andExpect(status().isNotFound());

        // Assert no partial booking was saved
        assertThat(bookingRepository.count()).isEqualTo(initialBookingCount + 1);
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("DB exclusion constraint: duplicate / overlapping booking on same room throws 409 Conflict")
    void testDoubleBookingExclusionConstraintConflict() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        LocalDate date = LocalDate.now().plusDays(40);

        CreateBookingRequest request1 = new CreateBookingRequest(
                "First Slot Booking",
                "Initial reservation",
                "ROOM",
                room.getId(),
                date,
                "11:00",
                "13:00",
                20,
                null,
                null,
                null,
                null
        );

        // First booking succeeds (201)
        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Overlapping booking on same room and overlapping hours (12:00 to 14:00) must return 409 CONFLICT
        CreateBookingRequest overlappingRequest = new CreateBookingRequest(
                "Overlapping Slot Attempt",
                "Conflicting reservation",
                "ROOM",
                room.getId(),
                date,
                "12:00",
                "14:00",
                25,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("already booked")));
    }

    @Test
    @DisplayName("Cancellation and IDOR: Owner can cancel (200), non-owner gets 404 same as non-existent ID")
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    void testCancellationAndIdorProtection() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        CreateBookingRequest request = new CreateBookingRequest(
                "Student Study Session",
                "Exam preparation group",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(45),
                "15:00",
                "17:00",
                15,
                null,
                null,
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // 1. Owner cancels booking -> 200 OK
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. IDOR check: Create another student booking, non-owner staff attempts to cancel -> 403 Forbidden
        CreateBookingRequest anotherRequest = new CreateBookingRequest(
                "Another Student Session",
                "Study group",
                "ROOM",
                room.getId(),
                LocalDate.now().plusDays(46),
                "15:00",
                "17:00",
                15,
                null,
                null,
                null,
                null
        );

        MvcResult anotherResult = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anotherRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String anotherBookingId = objectMapper.readTree(anotherResult.getResponse().getContentAsString()).get("id").asText();

        // Perform cancellation as Dr. Neha (STAFF) on Student's booking -> Must return 404 Not Found (same as non-existent ID)
        mockMvc.perform(post("/api/bookings/" + anotherBookingId + "/cancel")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Booking not found: " + anotherBookingId));
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("Admin approval queue, approval, and rejection with reason (>= 3 chars)")
    void testAdminApprovalWorkflow() throws Exception {
        // 1. Check approval queue has items
        mockMvc.perform(get("/api/admin/approvals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        // 2. Find a PENDING booking from the demo seed
        var pendingBooking = bookingRepository.findAll().stream()
                .filter(b -> "PENDING_APPROVAL".equals(b.getStatus()))
                .findFirst().orElseThrow();

        UUID bookingId = pendingBooking.getId();

        // 3. Admin approves booking
        mockMvc.perform(post("/api/admin/approvals/" + bookingId + "/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest("Approved by Admin", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 4. Find another PENDING booking to test rejection
        var anotherPending = bookingRepository.findAll().stream()
                .filter(b -> "PENDING_APPROVAL".equals(b.getStatus()))
                .findFirst().orElseThrow();

        // 5. Reject without reason (< 3 chars) -> 422 Unprocessable Entity
        mockMvc.perform(post("/api/admin/approvals/" + anotherPending.getId() + "/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest(null, "no"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        // 6. Reject with valid reason -> 200 OK
        mockMvc.perform(post("/api/admin/approvals/" + anotherPending.getId() + "/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalDecisionRequest(null, "Venue undergoing maintenance"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("CONCURRENCY: Two simultaneous booking requests for same room+slot: exactly one 201 and one 409")
    void testConcurrentDoubleBookingSameSlot_ExactlyOneSucceedsOneGets409() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        LocalDate date = LocalDate.now().plusDays(60);
        CreateBookingRequest request = new CreateBookingRequest(
                "Concurrent Test Alpha",
                "Concurrency conflict test",
                "ROOM",
                room.getId(),
                date,
                "16:00",
                "17:00",
                20,
                null, null, null, null
        );

        String payload = objectMapper.writeValueAsString(request);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger conflictCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Runnable task = () -> {
            try {
                startLatch.await();
                int status = mockMvc.perform(post("/api/bookings")
                                .with(csrf())
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andReturn().getResponse().getStatus();
                if (status == 201) successCount.incrementAndGet();
                else if (status == 409) conflictCount.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        startLatch.countDown(); // release both simultaneously
        t1.join(10_000);
        t2.join(10_000);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("CONCURRENCY: N+1 simultaneous parking requests on zone with capacity N: exactly N succeed")
    void testConcurrentParkingPermitN_plus1_ExactlyNSucceed() throws Exception {
        // Use a small capacity zone � set up a zone with 2 slots for this test
        // We use existing parking zones; just fire 3 simultaneous permit requests and check at most totalSlots succeed
        ParkingZone zone = parkingZoneRepository.findAll().get(0);
        int capacity = zone.getTotalSlots();
        int requests = capacity + 1;

        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger conflictCount = new java.util.concurrent.atomic.AtomicInteger(0);

        com.jklu.campusflow.campus.dto.ParkingPermitRequest permitRequest =
                new com.jklu.campusflow.campus.dto.ParkingPermitRequest(
                        "Concurrent Visitor",
                        "RJ-14-CC-" + zone.getCode(),
                        "FOUR_WHEELER",
                        zone.getId(),
                        LocalDate.now().plusDays(70),
                        "10:00",
                        "17:00",
                        "Concurrency Test"
                );
        String payload = objectMapper.writeValueAsString(permitRequest);

        Thread[] threads = new Thread[requests];
        for (int i = 0; i < requests; i++) {
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
                    int status = mockMvc.perform(post("/api/parking/permits")
                                    .with(csrf())
                                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                            .andReturn().getResponse().getStatus();
                    if (status == 201) successCount.incrementAndGet();
                    else if (status == 409) conflictCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }
        startLatch.countDown();
        for (Thread t : threads) t.join(15_000);

        // Exactly capacity permits succeed; exactly 1 conflicts
        assertThat(successCount.get()).isEqualTo(capacity);
        assertThat(conflictCount.get()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("CONFLICT RESPONSE: 409 on duplicate booking returns conflict shape with conflict message")
    void testDoubleBookingConflictReturnsConflictShape() throws Exception {
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> "CLASSROOM".equalsIgnoreCase(r.getType()) && r.isBookableOutside())
                .findFirst().orElseThrow();

        LocalDate date = LocalDate.now().plusDays(55);
        CreateBookingRequest first = new CreateBookingRequest(
                "First Booking", "First", "ROOM", room.getId(), date, "09:00", "11:00", 20, null, null, null, null);

        mockMvc.perform(post("/api/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CreateBookingRequest conflicting = new CreateBookingRequest(
                "Conflicting Booking", "Conflict", "ROOM", room.getId(), date, "10:00", "12:00", 20, null, null, null, null);

        mockMvc.perform(post("/api/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflicting)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message", containsString("already booked")))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.alternatives").isArray())
                .andExpect(jsonPath("$.nearbySlots").isArray());
    }
}