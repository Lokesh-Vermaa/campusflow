package com.jklu.campusflow.agent.planner;

import com.jklu.campusflow.agent.tools.AgentTools;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.common.security.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ScriptedPlanner {

    private static final Logger log = LoggerFactory.getLogger(ScriptedPlanner.class);

    private final AgentTools agentTools;
    private final AuditService auditService;

    public ScriptedPlanner(AgentTools agentTools, AuditService auditService) {
        this.agentTools = agentTools;
        this.auditService = auditService;
    }

    public void runDemoFlow(SseEmitter emitter, String userMessage, String userRole, UUID userId) {
        try {
            String reqId = RequestIdFilter.getCurrentRequestId();
            if (reqId == null) reqId = UUID.randomUUID().toString();

            // 1. Initial Plan Update
            send(emitter, "plan.update", "{\"steps\":[" +
                    "{\"id\":1,\"title\":\"Identify suitable venues for 80 attendees next Thursday 15:00–17:00\",\"status\":\"running\"}," +
                    "{\"id\":2,\"title\":\"Verify room availability against semester timetables\",\"status\":\"pending\"}," +
                    "{\"id\":3,\"title\":\"Locate guest room accommodation for visiting speaker\",\"status\":\"pending\"}," +
                    "{\"id\":4,\"title\":\"Check visitor parking zone allocation\",\"status\":\"pending\"}," +
                    "{\"id\":5,\"title\":\"Consult campus booking policy for approval requirements\",\"status\":\"pending\"}," +
                    "{\"id\":6,\"title\":\"Synthesize multi-resource proposal for human review\",\"status\":\"pending\"}" +
                    "]}");
            auditStep(userId, "PLAN_STEP", "1", "Identify suitable venues");

            // 2. Message Delta
            send(emitter, "message.delta", "{\"content\":\"Analyzing your request for a guest lecture next Thursday 15:00–17:00 for 80 attendees with accommodation and parking...\"}");

            // 3. Tool: find_free_rooms
            send(emitter, "tool.call.start", "{\"tool\":\"find_free_rooms\",\"args\":{\"capacity\":80,\"startTime\":\"15:00\",\"endTime\":\"17:00\"}}");
            auditTool(userId, "find_free_rooms", "{\"capacity\":80,\"startTime\":\"15:00\",\"endTime\":\"17:00\"}");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"find_free_rooms\",\"status\":\"success\",\"result\":[\"C-204\",\"M-201\",\"A-201\"]}");

            // 4. Tool: check_room_availability (Simulate temporary failure + auto-retry max 2)
            send(emitter, "tool.call.start", "{\"tool\":\"check_room_availability\",\"args\":{\"room\":\"C-204\"}}");
            auditTool(userId, "check_room_availability", "{\"room\":\"C-204\",\"attempt\":1}");
            Thread.sleep(100);
            // Simulated transient network issue
            send(emitter, "tool.call.end", "{\"tool\":\"check_room_availability\",\"status\":\"retry\",\"attempt\":1,\"message\":\"Database lock timeout; retrying (attempt 1/2)...\"}");
            Thread.sleep(100);
            // Retry succeeds
            send(emitter, "tool.call.start", "{\"tool\":\"check_room_availability\",\"args\":{\"room\":\"C-204\",\"retry\":true}}");
            auditTool(userId, "check_room_availability", "{\"room\":\"C-204\",\"attempt\":2}");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"check_room_availability\",\"status\":\"success\",\"result\":{\"room\":\"C-204\",\"conflict\":true,\"course\":\"Sem 3 Data Structures\",\"slot\":\"15:00-16:00\"}}");

            // 5. Conflict Card
            send(emitter, "card.conflict", "{\"conflict\":{" +
                    "\"resource\":\"Room C-204\"," +
                    "\"clashWith\":\"Sem 3 Data Structures (CS201) 15:00–16:00\"," +
                    "\"reason\":\"Regular semester timetable slots take strict priority over event bookings.\"" +
                    "}}");

            // 6. Tool: find_alternatives
            send(emitter, "tool.call.start", "{\"tool\":\"find_alternatives\",\"args\":{\"capacity\":80,\"slot\":\"Thursday 15:00-17:00\"}}");
            auditTool(userId, "find_alternatives", "{\"capacity\":80,\"slot\":\"Thursday 15:00-17:00\"}");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"find_alternatives\",\"status\":\"success\",\"alternatives\":[\"M-201 Management Amphitheatre (150 seats, free)\",\"A-201 Main Auditorium (120 seats, free)\"]}");

            // 7. Alternatives Card
            send(emitter, "card.alternatives", "{\"alternatives\":[" +
                    "{\"roomId\":\"00000000-0000-0000-0000-000000000030\",\"roomNumber\":\"M-201\",\"name\":\"Management Amphitheatre\",\"capacity\":150,\"features\":[\"projector\",\"ac\",\"audio_system\"]}," +
                    "{\"roomId\":\"00000000-0000-0000-0000-000000000023\",\"roomNumber\":\"A-201\",\"name\":\"Main Auditorium\",\"capacity\":120,\"features\":[\"projector\",\"ac\",\"audio_system\",\"stage\"]}" +
                    "]}");

            // 8. Tool: check_guest_house
            send(emitter, "tool.call.start", "{\"tool\":\"check_guest_house\",\"args\":{\"type\":\"SUITE\"}}");
            auditTool(userId, "check_guest_house", "{\"type\":\"SUITE\"}");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"check_guest_house\",\"status\":\"success\",\"result\":{\"roomId\":\"00000000-0000-0000-0000-000000000075\",\"roomNumber\":\"G-301\",\"type\":\"SUITE\",\"rate\":3500}}");

            // 9. Tool: check_parking
            send(emitter, "tool.call.start", "{\"tool\":\"check_parking\",\"args\":{\"zone\":\"P-VISITOR\"}}");
            auditTool(userId, "check_parking", "{\"zone\":\"P-VISITOR\"}");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"check_parking\",\"status\":\"success\",\"result\":{\"zoneId\":\"00000000-0000-0000-0000-000000000042\",\"code\":\"P-VISITOR\",\"status\":\"AVAILABLE\"}}");

            // 10. Tool: search_booking_policy
            send(emitter, "tool.call.start", "{\"tool\":\"search_booking_policy\",\"args\":{\"query\":\"approval threshold amphitheatre guest lecture\"}}");
            auditTool(userId, "search_booking_policy", "{\"query\":\"approval threshold amphitheatre guest lecture\"}");
            String policyExcerpt = agentTools.searchBookingPolicy("approval threshold");
            Thread.sleep(100);
            send(emitter, "tool.call.end", "{\"tool\":\"search_booking_policy\",\"status\":\"success\",\"result\":\"" + policyExcerpt.replace("\"", "\\\"").replace("\n", " ") + "\"}");

            // 11. Proposal Card (Never modifies database state — proposal only!)
            send(emitter, "card.proposal", "{\"proposal\":{" +
                    "\"title\":\"Guest Lecture — Speaker from Delhi\"," +
                    "\"room\":\"M-201 Management Amphitheatre\"," +
                    "\"roomId\":\"00000000-0000-0000-0000-000000000030\"," +
                    "\"guestHouse\":\"G-301 Suite (1 night)\"," +
                    "\"guestHouseRoomId\":\"00000000-0000-0000-0000-000000000075\"," +
                    "\"parking\":\"P-VISITOR Zone\"," +
                    "\"parkingZoneId\":\"00000000-0000-0000-0000-000000000042\"," +
                    "\"expectedAttendees\":80," +
                    "\"requiresApproval\":true," +
                    "\"preFilledPayload\":{" +
                    "\"title\":\"Guest Lecture — Speaker from Delhi\"," +
                    "\"description\":\"Guest lecture on Distributed Systems for 80 students.\"," +
                    "\"resourceType\":\"ROOM\"," +
                    "\"resourceId\":\"00000000-0000-0000-0000-000000000030\"," +
                    "\"startTime\":\"15:00\"," +
                    "\"endTime\":\"17:00\"," +
                    "\"expectedAttendees\":80," +
                    "\"guestHouseRoomId\":\"00000000-0000-0000-0000-000000000075\"," +
                    "\"parkingRequests\":[{\"visitorName\":\"Keynote Speaker\",\"vehicleNumber\":\"DL-01-AB-1234\",\"zoneId\":\"00000000-0000-0000-0000-000000000042\"}]" +
                    "}}}");

            // 12. Final Message Delta
            send(emitter, "message.delta", "{\"content\":\"I have assembled a complete bundle for your guest lecture: Room M-201 Management Amphitheatre (150 capacity, free of timetable clashes), Suite G-301 in MDC Guest House, and a Visitor Parking permit in P-VISITOR. Per campus policy, events with >50 attendees and amphitheatre bookings require Administrative approval. Click 'Submit for Approval' to send to admin.\"}");

            // 13. Done Event
            send(emitter, "done", "{\"content\":\"Proposal complete and ready for human confirmation.\"}");
            emitter.complete();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ScriptedPlanner execution interrupted");
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        } catch (IOException e) {
            log.warn("SSE emitter write error: {}", e.getMessage());
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    public void runClarifyingFlow(SseEmitter emitter, String userMessage, UUID userId) {
        try {
            send(emitter, "plan.update", "{\"steps\":[{\"id\":1,\"title\":\"Clarify missing event details\",\"status\":\"running\"}]}");
            send(emitter, "message.delta", "{\"content\":\"I'd be glad to help coordinate your campus reservation. Could you please specify: (1) The requested date, (2) Start and end times, and (3) Expected attendee count? Also let me know if guest house rooms or visitor parking are needed.\"}");
            send(emitter, "done", "{\"content\":\"Awaiting user clarification.\"}");
            emitter.complete();
        } catch (Exception e) {
            log.warn("Error running clarifying flow: {}", e.getMessage());
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    private void auditTool(UUID userId, String toolName, String args) {
        if (userId != null && auditService != null) {
            auditService.record(userId, "TOOL_CALL", "AGENT_TOOL", toolName, null, args, "127.0.0.1", "Agent/SSE");
        }
    }

    private void auditStep(UUID userId, String action, String stepId, String title) {
        if (userId != null && auditService != null) {
            auditService.record(userId, "AGENT_REASONING", "PLAN_STEP", stepId, null, "{\"title\":\"" + title + "\"}", "127.0.0.1", "Agent/SSE");
        }
    }

    private void send(SseEmitter emitter, String eventType, String data) throws IOException {
        emitter.send(SseEmitter.event().name(eventType).data(data));
    }
}
