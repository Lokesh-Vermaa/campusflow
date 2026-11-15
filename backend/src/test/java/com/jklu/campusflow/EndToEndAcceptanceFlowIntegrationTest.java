package com.jklu.campusflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.agent.dto.AgentChatRequest;
import com.jklu.campusflow.audit.model.AuditLog;
import com.jklu.campusflow.audit.repository.AuditLogRepository;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.booking.dto.ApprovalDecisionRequest;
import com.jklu.campusflow.booking.dto.BookingResponse;
import com.jklu.campusflow.booking.dto.CreateBookingRequest;
import com.jklu.campusflow.booking.dto.ParkingRequestItem;
import com.jklu.campusflow.booking.model.Booking;
import com.jklu.campusflow.booking.repository.BookingRepository;
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
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class EndToEndAcceptanceFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired AuditLogRepository auditLogRepository;

    private static final ZoneId ASIA_KOLKATA = ZoneId.of("Asia/Kolkata");

    @Test
    @DisplayName("PHASE 5 ACCEPTANCE FLOW: Staff AI Request -> Clash -> Alternatives -> Proposal -> Submit PENDING -> Admin Approval -> APPROVED in MyBookings -> Availability Unavailable -> Audit Verification")
    void testCompleteEndToEndAcceptanceFlow() throws Exception {
        LocalDate nextThursday = LocalDate.now(ASIA_KOLKATA).with(TemporalAdjusters.next(DayOfWeek.THURSDAY));

        // =========================================================================
        // Step 1 & 2: Staff asks AI Agent for Guest Lecture booking
        // =========================================================================
        AgentChatRequest chatReq = new AgentChatRequest(
                "Guest lecture next Thursday 3-5 PM, 80 people, speaker from Delhi needing stay and parking",
                null
        );

        MvcResult chatResult = mockMvc.perform(post("/api/agent/chat")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andReturn();

        String sseStream = chatResult.getResponse().getContentAsString();

        // Verify required contract SSE events streamed
        assertThat(sseStream).contains("event:plan");
        assertThat(sseStream).contains("event:thinking");
        assertThat(sseStream).contains("event:tool_call");
        assertThat(sseStream).contains("event:conflict");
        assertThat(sseStream).contains("event:alternatives");
        assertThat(sseStream).contains("event:proposal");
        assertThat(sseStream).contains("event:done");

        // Verify conflict detected at C-204 with Sem 3 Data Structures
        assertThat(sseStream).contains("C-204");
        assertThat(sseStream).contains("CS201");

        // Extract preFilledPayload from the proposal event
        String proposalPrefix = "event:proposal\ndata:";
        int proposalIdx = sseStream.indexOf(proposalPrefix);
        assertThat(proposalIdx).isGreaterThanOrEqualTo(0);
        int proposalEnd = sseStream.indexOf("\n\n", proposalIdx);
        String proposalJson = sseStream.substring(proposalIdx + proposalPrefix.length(), proposalEnd).trim();

        JsonNode rootNode = objectMapper.readTree(proposalJson);
        JsonNode proposalNode = rootNode.get("proposal");
        assertThat(proposalNode).isNotNull();
        JsonNode payloadNode = proposalNode.get("preFilledPayload");
        assertThat(payloadNode).isNotNull();

        UUID selectedRoomId = UUID.fromString(payloadNode.get("resourceId").asText());
        int attendees = payloadNode.get("expectedAttendees").asInt();
        String bookingDate = payloadNode.get("bookingDate").asText();
        String startTime = payloadNode.get("startTime").asText();
        String endTime = payloadNode.get("endTime").asText();
        UUID guestHouseRoomId = payloadNode.has("guestHouseRoomId") ? UUID.fromString(payloadNode.get("guestHouseRoomId").asText()) : null;

        List<ParkingRequestItem> parkingRequests = null;
        if (payloadNode.has("parkingRequests") && payloadNode.get("parkingRequests").isArray()) {
            JsonNode pArr = payloadNode.get("parkingRequests");
            if (pArr.size() > 0) {
                JsonNode p0 = pArr.get(0);
                parkingRequests = List.of(new ParkingRequestItem(
                        p0.get("visitorName").asText(),
                        p0.get("vehicleNumber").asText(),
                        UUID.fromString(p0.get("zoneId").asText())
                ));
            }
        }

        // =========================================================================
        // Step 3: Staff submits the Proposal for Approval -> creates PENDING booking
        // =========================================================================
        CreateBookingRequest createRequest = new CreateBookingRequest(
                payloadNode.get("title").asText(),
                payloadNode.get("description").asText(),
                "ROOM",
                selectedRoomId,
                LocalDate.parse(bookingDate),
                startTime,
                endTime,
                attendees,
                guestHouseRoomId,
                LocalDate.parse(bookingDate),
                LocalDate.parse(bookingDate).plusDays(1),
                parkingRequests
        );

        String idempotencyKey = UUID.randomUUID().toString();
        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF"))
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        BookingResponse createdBooking = objectMapper.readValue(bookingResult.getResponse().getContentAsString(), BookingResponse.class);
        UUID bookingId = createdBooking.id();
        assertThat(bookingId).isNotNull();

        // In DB it must be PENDING_APPROVAL
        Booking dbBookingBefore = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(dbBookingBefore.getStatus()).isEqualTo("PENDING_APPROVAL");

        // =========================================================================
        // Step 4: Admin views Approval Queue -> booking is present
        // =========================================================================
        mockMvc.perform(get("/api/admin/approvals?page=1&pageSize=10")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@demo.jklu.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + bookingId + "')]").exists());

        // =========================================================================
        // Step 5: Admin Approves the Booking
        // =========================================================================
        ApprovalDecisionRequest approveReq = new ApprovalDecisionRequest("Approved by Campus Operations for visiting keynote guest.", null);
        mockMvc.perform(post("/api/admin/approvals/" + bookingId + "/approve")
                        .with(csrf())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@demo.jklu.test").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // In DB it must be CONFIRMED
        Booking dbBookingAfter = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(dbBookingAfter.getStatus()).isEqualTo("CONFIRMED");

        // =========================================================================
        // Step 6: Staff retrieves My Bookings -> booking is APPROVED
        // =========================================================================
        mockMvc.perform(get("/api/bookings/my")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '" + bookingId + "')].status").value("APPROVED"));

        // =========================================================================
        // Step 7: Availability Engine -> Room is NO LONGER available for that slot
        // =========================================================================
        mockMvc.perform(get("/api/rooms/availability")
                        .param("date", bookingDate)
                        .param("startTime", startTime)
                        .param("endTime", endTime)
                        .param("minCapacity", "50")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("staff@demo.jklu.test").roles("STAFF")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableRooms[?(@.id == '" + selectedRoomId + "')]").doesNotExist())
                .andExpect(jsonPath("$.unavailableRooms[?(@.id == '" + selectedRoomId + "')]").exists());

        // =========================================================================
        // Step 8: Admin views Audit Logs -> shows approval and agent steps
        // =========================================================================
        MvcResult auditResult = mockMvc.perform(get("/api/admin/audit?pageSize=50")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@demo.jklu.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        String auditResponse = auditResult.getResponse().getContentAsString();
        assertThat(auditResponse).contains("APPROVAL");
        assertThat(auditResponse).contains("AGENT_REASONING");
        assertThat(auditResponse).contains("TOOL_CALL");
    }
}
