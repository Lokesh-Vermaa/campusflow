package com.jklu.campusflow.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.BaseIntegrationTest;
import com.jklu.campusflow.agent.dto.AgentChatRequest;
import com.jklu.campusflow.agent.model.ConversationSession;
import com.jklu.campusflow.agent.planner.AgentRequestParser;
import com.jklu.campusflow.agent.repository.ConversationSessionRepository;
import com.jklu.campusflow.audit.repository.AuditLogRepository;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.booking.dto.CreateBookingRequest;
import com.jklu.campusflow.booking.model.Booking;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.booking.repository.BookingItemRepository;
import com.jklu.campusflow.booking.repository.BookingRepository;
import com.jklu.campusflow.booking.service.BookingService;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AgentDataDrivenPlannerIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RoomRepository roomRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired BookingItemRepository bookingItemRepository;
    @Autowired BookingService bookingService;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired ConversationSessionRepository conversationSessionRepository;

    private static final ZoneId ASIA_KOLKATA = ZoneId.of("Asia/Kolkata");

    @Test
    @DisplayName("PARSER: Relative dates are parsed strictly using Asia/Kolkata timezone")
    void testRelativeDateParsingAsiaKolkata() {
        LocalDate baseDate = LocalDate.of(2026, 11, 15); // Sunday
        AgentRequestParser.ParsedEventRequest parsed = AgentRequestParser.parse(
                "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking",
                baseDate
        );

        LocalDate expectedThursday = baseDate.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        assertThat(parsed.date()).isEqualTo(expectedThursday);
        assertThat(parsed.startTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(parsed.endTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(parsed.headcount()).isEqualTo(80);
        assertThat(parsed.needsGuestHouse()).isTrue();
        assertThat(parsed.needsParking()).isTrue();
        assertThat(parsed.missingRequiredFields()).isFalse();
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("PLANS (A) & (B): Requests produce DIFFERENT plans and tool-call lists")
    void testDifferentRequestsProduceDifferentPlansAndToolCalls() throws Exception {
        // Request A: 80 people, guest house, parking
        AgentChatRequest reqA = new AgentChatRequest(
                "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking",
                null
        );

        MvcResult resultA = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(request().asyncStarted())
                .andReturn();
        resultA.getAsyncResult(15_000L);
        String streamA = resultA.getResponse().getContentAsString();

        // Request B: 40 people, no guest house or parking
        AgentChatRequest reqB = new AgentChatRequest(
                "room for 40 people next Friday 10-11 AM, no guest house or parking",
                null
        );

        MvcResult resultB = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(request().asyncStarted())
                .andReturn();
        resultB.getAsyncResult(15_000L);
        String streamB = resultB.getResponse().getContentAsString();

        // Stream A must contain guest house and parking tools/steps
        assertThat(streamA).contains("reserveGuestHouseRoom");
        assertThat(streamA).contains("generateVisitorParkingPermit");
        assertThat(streamA).contains("event:conflict");

        // Stream B must NOT contain guest house or parking tools/steps
        assertThat(streamB).doesNotContain("reserveGuestHouseRoom");
        assertThat(streamB).doesNotContain("generateVisitorParkingPermit");

        // The streams are strictly different
        assertThat(streamA).isNotEqualTo(streamB);
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("NO CONFLICT: Free slot returns proposal WITHOUT conflict or alternative cards")
    void testRequestWithoutConflictReturnsProposalDirectly() throws Exception {
        AgentChatRequest req = new AgentChatRequest(
                "room for 40 people next Friday 10-11 AM, no guest house or parking",
                null
        );

        MvcResult result = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();
        result.getAsyncResult(15_000L);

        String stream = result.getResponse().getContentAsString();
        assertThat(stream).contains("event:plan");
        assertThat(stream).contains("event:proposal");
        assertThat(stream).contains("event:done");

        // Must NOT emit conflict or alternatives card when there is no clash
        assertThat(stream).doesNotContain("event:conflict");
        assertThat(stream).doesNotContain("event:alternatives");
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("DYNAMIC DATA CHANGE: Adding booking dynamically changes planner outcome")
    void testAddingBookingChangesPlannerOutcome() throws Exception {
        // Step 1: Query for Friday 10-11 AM -> normally no clash
        LocalDate nextFriday = LocalDate.now(ASIA_KOLKATA).with(TemporalAdjusters.next(DayOfWeek.FRIDAY));
        String prompt = "room for 40 people " + nextFriday + " 10-11 AM, no guest house or parking";

        MvcResult res1 = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(new AgentChatRequest(prompt, null))))
                .andExpect(request().asyncStarted())
                .andReturn();
        res1.getAsyncResult(15_000L);
        Thread.sleep(200);
        String output1 = res1.getResponse().getContentAsString();
        assertThat(output1).doesNotContain("event:conflict");

        // Step 2: Dynamically insert an approved booking for the 40-capacity room on that Friday
        User staff = userRepository.findByEmailIgnoreCase("staff@demo.jklu.test").orElseThrow();
        Room room = roomRepository.findAllActiveWithBlock().stream()
                .filter(r -> r.getCapacity() >= 40)
                .min((a, b) -> Integer.compare(a.getCapacity(), b.getCapacity()))
                .orElseThrow();

        Booking booking = new Booking(null, "BK-TEST-CONF", staff, "Conflicting Event", "Blocked slot", "CONFIRMED", "Campus Event", 40);
        booking = bookingRepository.save(booking);

        OffsetDateTime startTz = nextFriday.atTime(10, 0).atZone(ASIA_KOLKATA).toOffsetDateTime();
        OffsetDateTime endTz = nextFriday.atTime(11, 0).atZone(ASIA_KOLKATA).toOffsetDateTime();
        BookingItem item = new BookingItem(booking.getId(), "ROOM", room.getId(), startTz, endTz, "CONFIRMED");
        bookingItemRepository.save(item);

        // Step 3: Run the exact same query again -> planner must now dynamically detect the clash!
        MvcResult res2 = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(new AgentChatRequest(prompt, null))))
                .andExpect(request().asyncStarted())
                .andReturn();
        res2.getAsyncResult(15_000L);
        String output2 = res2.getResponse().getContentAsString();

        assertThat(output2).contains("event:conflict");
        assertThat(output2).contains("event:alternatives");
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("MISSING FIELDS: Missing slot triggers clarifying question")
    void testMissingSlotTriggersClarifyingQuestion() throws Exception {
        AgentChatRequest req = new AgentChatRequest("I need to book a room", null);

        MvcResult res = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();
        res.getAsyncResult(15_000L);

        String stream = res.getResponse().getContentAsString();
        assertThat(stream).contains("Clarify missing event specifications");
        assertThat(stream).contains("Could you please specify");
        assertThat(stream).doesNotContain("event:proposal");
    }

    @Test
    @WithMockUser(username = "admin@demo.jklu.test", roles = {"ADMIN"})
    @DisplayName("AUDIT: Plan steps write AGENT_REASONING and tool calls write TOOL_CALL filterable in audit endpoint")
    void testAuditAgentStepsAndToolCallsAreFilterable() throws Exception {
        AgentChatRequest req = new AgentChatRequest(
                "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking",
                null
        );

        MvcResult res = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();
        res.getAsyncResult(15_000L);

        // Verify audit logs via GET /api/admin/audit?eventType=AGENT_REASONING
        mockMvc.perform(get("/api/admin/audit")
                        .param("eventType", "AGENT_REASONING")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("AGENT_REASONING");
                });

        // Verify audit logs via GET /api/admin/audit?eventType=TOOL_CALL
        mockMvc.perform(get("/api/admin/audit")
                        .param("eventType", "TOOL_CALL")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).contains("TOOL_CALL");
                });
    }

    @Test
    @WithMockUser(username = "student@demo.jklu.test", roles = {"STUDENT"})
    @DisplayName("ROLE RESTRICTION: Student cannot see/book labs or amphitheatres through agent or booking service")
    void testStudentCannotBookRestrictedVenues() throws Exception {
        // Query as student for 150 people (only amphitheatres have 150 capacity)
        AgentChatRequest req = new AgentChatRequest(
                "room for 150 people next Friday 10-11 AM, no guest house or parking",
                null
        );

        MvcResult res = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();
        res.getAsyncResult(15_000L);

        String stream = res.getResponse().getContentAsString();
        // Since student can only see CLASSROOMs, no classroom has 150 capacity
        assertThat(stream).contains("No active venues available with capacity >= 150 for role STUDENT");

        // Prove BookingService forbids Student from creating booking for an Amphitheatre
        User student = userRepository.findByEmailIgnoreCase("student@demo.jklu.test").orElseThrow();
        Room amphitheatre = roomRepository.findAllActiveVenuesWithBlock().stream()
                .filter(r -> "AMPHITHEATRE".equalsIgnoreCase(r.getType()))
                .findFirst()
                .orElseThrow();

        CreateBookingRequest bookingReq = new CreateBookingRequest(
                "Student Amphitheatre Attempt",
                "Testing role restriction",
                "ROOM",
                amphitheatre.getId(),
                LocalDate.now().plusDays(5),
                "10:00",
                "11:00",
                100,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> bookingService.createBooking(bookingReq, student.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Students are only permitted to book standard CLASSROOM venues");
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("PROPOSAL SUBMISSION: Submit for approval calls BookingService and creates PENDING_APPROVAL booking")
    void testProposalSubmissionCreatesPendingApprovalBooking() throws Exception {
        User staff = userRepository.findByEmailIgnoreCase("staff@demo.jklu.test").orElseThrow();
        Room room = roomRepository.findAllActiveClassroomsWithBlock().get(0);

        LocalDate nextWed = LocalDate.now(ASIA_KOLKATA).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        CreateBookingRequest bookingReq = new CreateBookingRequest(
                "Proposal Submitted Event",
                "From agent proposal submission",
                "ROOM",
                room.getId(),
                nextWed,
                "14:00",
                "15:00",
                40,
                null,
                null,
                null,
                null
        );

        var bookingResponse = bookingService.createBooking(bookingReq, staff.getId());

        // Contract requires PENDING status, never auto-approved
        assertThat(bookingResponse.status()).isEqualTo("PENDING");

        Booking dbBooking = bookingRepository.findById(bookingResponse.id()).orElseThrow();
        assertThat(dbBooking.getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    @WithMockUser(username = "staff@demo.jklu.test", roles = {"STAFF"})
    @DisplayName("CONVERSATION MESSAGES ENDPOINT: POST /api/conversations/{id}/messages streams SSE identically")
    void testConversationMessagesSseStream() throws Exception {
        User staff = userRepository.findByEmailIgnoreCase("staff@demo.jklu.test").orElseThrow();
        ConversationSession session = conversationSessionRepository.save(new ConversationSession(staff, "SSE Test Conversation"));

        AgentChatRequest req = new AgentChatRequest(
                "room for 40 people next Friday 10-11 AM, no guest house or parking",
                session.getId()
        );

        MvcResult res = mockMvc.perform(post("/api/conversations/" + session.getId() + "/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();
        res.getAsyncResult(15_000L);

        String stream = res.getResponse().getContentAsString();
        assertThat(stream).contains("event:plan");
        assertThat(stream).contains("event:proposal");
        assertThat(stream).contains("event:done");
    }
}
