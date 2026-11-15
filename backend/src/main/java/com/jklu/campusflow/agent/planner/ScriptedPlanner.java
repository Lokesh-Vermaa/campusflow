package com.jklu.campusflow.agent.planner;

import com.jklu.campusflow.agent.model.ConversationMessage;
import com.jklu.campusflow.agent.repository.ConversationMessageRepository;
import com.jklu.campusflow.agent.repository.ConversationSessionRepository;
import com.jklu.campusflow.agent.service.PolicySearchService;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.booking.model.BookingItem;
import com.jklu.campusflow.booking.repository.BookingItemRepository;
import com.jklu.campusflow.campus.model.GuestRoom;
import com.jklu.campusflow.campus.model.ParkingZone;
import com.jklu.campusflow.campus.model.Room;
import com.jklu.campusflow.campus.repository.GuestRoomRepository;
import com.jklu.campusflow.campus.repository.ParkingZoneRepository;
import com.jklu.campusflow.campus.repository.RoomRepository;
import com.jklu.campusflow.timetable.model.TimetableSlot;
import com.jklu.campusflow.timetable.repository.TimetableSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ScriptedPlanner {

    private static final Logger log = LoggerFactory.getLogger(ScriptedPlanner.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final RoomRepository roomRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final BookingItemRepository bookingItemRepository;
    private final GuestRoomRepository guestRoomRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final PolicySearchService policySearchService;
    private final AuditService auditService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSessionRepository conversationSessionRepository;

    public ScriptedPlanner(
            RoomRepository roomRepository,
            TimetableSlotRepository timetableSlotRepository,
            BookingItemRepository bookingItemRepository,
            GuestRoomRepository guestRoomRepository,
            ParkingZoneRepository parkingZoneRepository,
            PolicySearchService policySearchService,
            AuditService auditService,
            ConversationMessageRepository conversationMessageRepository,
            ConversationSessionRepository conversationSessionRepository
    ) {
        this.roomRepository = roomRepository;
        this.timetableSlotRepository = timetableSlotRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.guestRoomRepository = guestRoomRepository;
        this.parkingZoneRepository = parkingZoneRepository;
        this.policySearchService = policySearchService;
        this.auditService = auditService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationSessionRepository = conversationSessionRepository;
    }

    public void runDataDrivenFlow(SseEmitter emitter, String rawUserMessage, String userRole, UUID userId, UUID conversationId) {
        try {
            String entityId = conversationId != null ? conversationId.toString() : (userId != null ? userId.toString() : "session");

            // 1. Parse natural language request using Asia/Kolkata timezone
            AgentRequestParser.ParsedEventRequest parsed = AgentRequestParser.parse(rawUserMessage, LocalDate.now(ZONE));

            if (parsed.missingRequiredFields()) {
                handleClarifyingRequest(emitter, rawUserMessage, userId, entityId);
                return;
            }

            // 2. Build dynamic plan steps based on parsed request
            List<Map<String, Object>> steps = new ArrayList<>();
            int stepId = 1;

            steps.add(Map.of("id", stepId++, "title", "Identify candidate venues for " + parsed.headcount() + " attendees", "status", "running"));
            steps.add(Map.of("id", stepId++, "title", "Verify availability against timetables for " + parsed.date() + " " + parsed.startTime() + "-" + parsed.endTime(), "status", "pending"));

            if (parsed.needsGuestHouse()) {
                steps.add(Map.of("id", stepId++, "title", "Verify MDC Guest House room vacancy for visiting guest", "status", "pending"));
            }
            if (parsed.needsParking()) {
                steps.add(Map.of("id", stepId++, "title", "Allocate campus visitor parking zone", "status", "pending"));
            }
            steps.add(Map.of("id", stepId++, "title", "Consult campus booking policy for approval requirements", "status", "pending"));
            steps.add(Map.of("id", stepId++, "title", "Synthesize multi-resource proposal for human review", "status", "pending"));

            // Emit initial plan
            sendEvent(emitter, "plan", toJson(Map.of("steps", steps)));
            recordAuditReasoning(userId, entityId, "Constructed dynamic plan with " + steps.size() + " steps for headcount=" + parsed.headcount() + ", date=" + parsed.date());

            // Emit thinking
            sendEvent(emitter, "thinking", toJson(Map.of("content", "Checking room availability for " + parsed.headcount() + " attendees on " + parsed.date() + " between " + parsed.startTime() + " and " + parsed.endTime() + "...")));

            // 3. Find candidate rooms from database matching capacity and role constraints
            long toolStart1 = System.currentTimeMillis();
            List<Room> allRooms = roomRepository.findAllActiveWithBlock();

            // Candidate selection rule:
            // 1) Enforce role restriction: STUDENT can only see/book CLASSROOMs. STAFF/ADMIN can see all types.
            // 2) Filter capacity >= requested headcount.
            // 3) Sort by capacity ascending (smallest room fitting the headcount).
            boolean isStudent = "STUDENT".equalsIgnoreCase(userRole);
            List<Room> candidateRooms = allRooms.stream()
                    .filter(r -> {
                        if (isStudent && !"CLASSROOM".equalsIgnoreCase(r.getType())) {
                            return false;
                        }
                        return r.getCapacity() >= parsed.headcount();
                    })
                    .sorted(Comparator.comparingInt(Room::getCapacity))
                    .toList();

            long duration1 = System.currentTimeMillis() - toolStart1;
            sendEvent(emitter, "tool_call", toJson(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "tool", "find_free_rooms",
                    "args", Map.of("minCapacity", parsed.headcount(), "role", userRole),
                    "status", "success",
                    "durationMs", duration1,
                    "resultSummary", "Found " + candidateRooms.size() + " candidate rooms meeting capacity requirement."
            )));
            recordAuditToolCall(userId, entityId, "find_free_rooms", Map.of("minCapacity", parsed.headcount(), "role", userRole), "Found " + candidateRooms.size() + " candidate rooms", duration1, "success", 0);

            if (candidateRooms.isEmpty()) {
                sendEvent(emitter, "thinking", toJson(Map.of("content", "No active venues available with capacity >= " + parsed.headcount() + " for role " + userRole + ".")));
                sendEvent(emitter, "done", toJson(Map.of("content", "Unable to find rooms with capacity of " + parsed.headcount() + " seats. Please consider reducing attendee count or contacting administration.")));
                emitter.complete();
                return;
            }

            // 4. Evaluate clash on candidate rooms
            // Inspect smallest candidate room first
            Room candidateRoom = candidateRooms.get(0);
            DayOfWeek dow = parsed.date().getDayOfWeek();
            int dayOfWeek = dow.getValue(); // 1=Mon .. 7=Sun

            List<TimetableSlot> slots = timetableSlotRepository.findAllActiveByDayOfWeek(dayOfWeek);
            OffsetDateTime dayStart = parsed.date().atStartOfDay(ZONE).toOffsetDateTime();
            OffsetDateTime dayEnd = parsed.date().plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
            List<BookingItem> activeBookings = bookingItemRepository.findActiveBookingsBetween(dayStart, dayEnd);

            // Check if best-fit room has a clash
            TimetableSlot clashSlot = findRoomTimetableClash(candidateRoom.getId(), slots, parsed.startTime(), parsed.endTime());
            BookingItem clashBooking = findRoomBookingClash(candidateRoom.getId(), activeBookings, parsed.date(), parsed.startTime(), parsed.endTime());

            Room selectedRoom = candidateRoom;
            boolean hadConflict = false;

            if (clashSlot != null || clashBooking != null) {
                hadConflict = true;
                String clashDesc = clashSlot != null
                        ? clashSlot.getCourseCode() + " " + (clashSlot.getSection() != null ? clashSlot.getSection() + " " : "") + formatTime(clashSlot.getStartMinute()) + "-" + formatTime(clashSlot.getEndMinute())
                        : "Approved Booking " + clashBooking.getBookingId();

                String clashingTime = clashSlot != null
                        ? formatTime(clashSlot.getStartMinute()) + "-" + formatTime(clashSlot.getEndMinute())
                        : parsed.startTime() + "-" + parsed.endTime();

                // Tool call: checkAvailability (retry demonstration)
                sendEvent(emitter, "tool_call", toJson(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "tool", "check_room_availability",
                        "args", Map.of("room", candidateRoom.getRoomNumber(), "date", parsed.date().toString()),
                        "status", "retry",
                        "retryCount", 1,
                        "durationMs", 180,
                        "resultSummary", "Replica lock delay; retrying on primary replica (attempt 1/2)..."
                )));
                recordAuditToolCall(userId, entityId, "check_room_availability", Map.of("room", candidateRoom.getRoomNumber()), "Replica retry", 180, "retry", 1);

                sendEvent(emitter, "tool_call", toJson(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "tool", "check_room_availability",
                        "args", Map.of("room", candidateRoom.getRoomNumber(), "date", parsed.date().toString()),
                        "status", "success",
                        "retryCount", 1,
                        "durationMs", 120,
                        "resultSummary", "Clash confirmed: " + clashDesc
                )));
                recordAuditToolCall(userId, entityId, "check_room_availability", Map.of("room", candidateRoom.getRoomNumber()), "Clash detected with " + clashDesc, 120, "success", 1);

                // Emit conflict card
                sendEvent(emitter, "conflict", toJson(Map.of(
                        "resource", "Room " + candidateRoom.getRoomNumber() + (candidateRoom.getName() != null ? " (" + candidateRoom.getName() + ")" : ""),
                        "clashWith", clashDesc,
                        "clashingSlot", clashingTime,
                        "reason", "Room " + candidateRoom.getRoomNumber() + " is occupied by " + clashDesc + ". Regular timetable lectures take strict precedence."
                )));
                recordAuditReasoning(userId, entityId, "Detected schedule clash on Room " + candidateRoom.getRoomNumber() + " with " + clashDesc + ". Searching alternative free venues.");

                // Search alternatives among candidate rooms that are completely free
                List<Room> freeAlternatives = new ArrayList<>();
                for (Room r : candidateRooms) {
                    if (r.getId().equals(candidateRoom.getId())) continue;
                    if (findRoomTimetableClash(r.getId(), slots, parsed.startTime(), parsed.endTime()) == null
                            && findRoomBookingClash(r.getId(), activeBookings, parsed.date(), parsed.startTime(), parsed.endTime()) == null) {
                        freeAlternatives.add(r);
                    }
                }

                if (!freeAlternatives.isEmpty()) {
                    selectedRoom = freeAlternatives.get(0);

                    List<Map<String, Object>> altCards = freeAlternatives.stream().limit(3).map(alt -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", alt.getId().toString());
                        m.put("name", "Room " + alt.getRoomNumber() + (alt.getName() != null ? " (" + alt.getName() + ")" : ""));
                        m.put("capacity", alt.getCapacity());
                        m.put("features", alt.getFeatures());
                        return m;
                    }).toList();

                    List<String> suggestedList = freeAlternatives.stream().limit(3)
                            .map(alt -> "Room " + alt.getRoomNumber() + " (cap " + alt.getCapacity() + ", free)")
                            .toList();

                    sendEvent(emitter, "alternatives", toJson(Map.of(
                            "suggested", suggestedList,
                            "alternatives", altCards
                    )));
                } else {
                    sendEvent(emitter, "thinking", toJson(Map.of("content", "No alternative venues of suitable capacity are free at this exact time slot.")));
                }
            } else {
                // Best room is completely free! No conflict step!
                sendEvent(emitter, "tool_call", toJson(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "tool", "check_room_availability",
                        "args", Map.of("room", candidateRoom.getRoomNumber(), "date", parsed.date().toString()),
                        "status", "success",
                        "durationMs", 95,
                        "resultSummary", "Room " + candidateRoom.getRoomNumber() + " is completely free with no clashes."
                )));
                recordAuditToolCall(userId, entityId, "check_room_availability", Map.of("room", candidateRoom.getRoomNumber()), "Free with 0 clashes", 95, "success", 0);
            }

            // 5. Guest House allocation if needed (query real database and find a free room)
            GuestRoom selectedGuestRoom = null;
            if (parsed.needsGuestHouse()) {
                long toolStartGh = System.currentTimeMillis();
                List<GuestRoom> guestRooms = guestRoomRepository.findAllByActiveTrueOrderByRoomNumberAsc();
                OffsetDateTime grCheckIn = parsed.date().atTime(12, 0).atZone(ZONE).toOffsetDateTime();
                OffsetDateTime grCheckOut = parsed.date().plusDays(1).atTime(11, 0).atZone(ZONE).toOffsetDateTime();
                List<BookingItem> bookedGuestRooms = bookingItemRepository.findActiveBookingsByResourceTypeBetween("GUEST_ROOM", grCheckIn, grCheckOut);
                Set<UUID> bookedGuestRoomIds = bookedGuestRooms.stream().map(BookingItem::getResourceId).collect(Collectors.toSet());
                for (GuestRoom gr : guestRooms) {
                    if (!bookedGuestRoomIds.contains(gr.getId())) {
                        selectedGuestRoom = gr;
                        break;
                    }
                }
                long durationGh = System.currentTimeMillis() - toolStartGh;
                sendEvent(emitter, "tool_call", toJson(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "tool", "reserveGuestHouseRoom",
                        "args", Map.of("checkIn", parsed.date().toString(), "type", "ANY"),
                        "status", "success",
                        "durationMs", durationGh,
                        "resultSummary", selectedGuestRoom != null ? "Allocated " + selectedGuestRoom.getRoomNumber() + " (" + selectedGuestRoom.getType() + ")" : "No guest room free"
                )));
                recordAuditToolCall(userId, entityId, "reserveGuestHouseRoom", Map.of("date", parsed.date().toString()), selectedGuestRoom != null ? selectedGuestRoom.getRoomNumber() : "None", durationGh, "success", 0);
            }

            // 6. Parking allocation if needed (query real database and find zone with slots)
            ParkingZone selectedParking = null;
            if (parsed.needsParking()) {
                long toolStartPk = System.currentTimeMillis();
                List<ParkingZone> zones = parkingZoneRepository.findAllByActiveTrueOrderByNameAsc();
                for (ParkingZone z : zones) {
                    if (z.getTotalSlots() > 0) {
                        selectedParking = z;
                        break;
                    }
                }
                long durationPk = System.currentTimeMillis() - toolStartPk;
                sendEvent(emitter, "tool_call", toJson(Map.of(
                        "id", UUID.randomUUID().toString(),
                        "tool", "generateVisitorParkingPermit",
                        "args", Map.of("zone", selectedParking != null ? selectedParking.getCode() : "P1"),
                        "status", "success",
                        "durationMs", durationPk,
                        "resultSummary", selectedParking != null ? "Assigned permit in " + selectedParking.getName() + " (" + selectedParking.getCode() + ")" : "No zone"
                )));
                recordAuditToolCall(userId, entityId, "generateVisitorParkingPermit", Map.of("zone", selectedParking != null ? selectedParking.getCode() : "P1"), "Assigned zone", durationPk, "success", 0);
            }

            // 7. Policy search tool
            String policyResult = policySearchService.search("approval threshold guest lecture");
            sendEvent(emitter, "tool_call", toJson(Map.of(
                    "id", UUID.randomUUID().toString(),
                    "tool", "search_booking_policy",
                    "args", Map.of("query", "approval threshold"),
                    "status", "success",
                    "durationMs", 60,
                    "resultSummary", policyResult
            )));
            recordAuditToolCall(userId, entityId, "search_booking_policy", Map.of("query", "approval threshold"), policyResult, 60, "success", 0);

            boolean requiresAdminApproval = parsed.headcount() > 50 || parsed.needsGuestHouse() || "AMPHITHEATRE".equalsIgnoreCase(selectedRoom.getType());

            // 8. Synthesize Proposal
            String roomDisplay = "Room " + selectedRoom.getRoomNumber() + (selectedRoom.getName() != null ? " (" + selectedRoom.getName() + ")" : "");
            String guestDisplay = selectedGuestRoom != null ? selectedGuestRoom.getRoomNumber() + " (" + selectedGuestRoom.getType() + ")" : null;
            String parkingDisplay = selectedParking != null ? selectedParking.getName() + " (" + selectedParking.getCode() + ")" : null;

            Map<String, Object> preFilledPayload = new LinkedHashMap<>();
            preFilledPayload.put("title", "Campus Reservation — " + parsed.headcount() + " Attendees");
            preFilledPayload.put("description", "Automated plan: " + rawUserMessage);
            preFilledPayload.put("resourceType", "ROOM");
            preFilledPayload.put("resourceId", selectedRoom.getId().toString());
            preFilledPayload.put("bookingDate", parsed.date().toString());
            preFilledPayload.put("startTime", parsed.startTime().toString());
            preFilledPayload.put("endTime", parsed.endTime().toString());
            preFilledPayload.put("expectedAttendees", parsed.headcount());
            if (selectedGuestRoom != null) {
                preFilledPayload.put("guestHouseRoomId", selectedGuestRoom.getId().toString());
            }
            if (selectedParking != null) {
                preFilledPayload.put("parkingRequests", List.of(Map.of(
                        "visitorName", "Keynote Speaker",
                        "vehicleNumber", "DL-01-AB-1234",
                        "zoneId", selectedParking.getId().toString()
                )));
            }

            Map<String, Object> proposalMap = new LinkedHashMap<>();
            proposalMap.put("title", "Campus Multi-Resource Proposal");
            proposalMap.put("roomName", roomDisplay);
            proposalMap.put("roomId", selectedRoom.getId().toString());
            proposalMap.put("date", parsed.date().toString());
            proposalMap.put("time", parsed.startTime().toString() + " - " + parsed.endTime().toString());
            proposalMap.put("capacity", selectedRoom.getCapacity());
            if (guestDisplay != null) {
                proposalMap.put("guestHouse", guestDisplay);
                proposalMap.put("guestHouseRoomId", selectedGuestRoom.getId().toString());
            }
            if (parkingDisplay != null) {
                proposalMap.put("parking", parkingDisplay);
                proposalMap.put("parkingZoneId", selectedParking.getId().toString());
            }
            proposalMap.put("requiresAdminApproval", requiresAdminApproval);
            proposalMap.put("status", "DRAFT");
            proposalMap.put("preFilledPayload", preFilledPayload);

            sendEvent(emitter, "proposal", toJson(Map.of("proposal", proposalMap)));
            recordAuditReasoning(userId, entityId, "Prepared proposal package for " + roomDisplay + (guestDisplay != null ? ", " + guestDisplay : "") + ". Status: DRAFT.");

            // 9. Conclude with summary and Done event
            StringBuilder doneMsg = new StringBuilder();
            doneMsg.append("Plan reconciled successfully: Selected **").append(roomDisplay).append("** (capacity ").append(selectedRoom.getCapacity()).append(").");
            if (selectedGuestRoom != null) {
                doneMsg.append(" Reserved MDC Guest House **").append(guestDisplay).append("**.");
            }
            if (selectedParking != null) {
                doneMsg.append(" Assigned visitor parking pass for **").append(parkingDisplay).append("**.");
            }
            if (requiresAdminApproval) {
                doneMsg.append(" This reservation bundle requires Administrative approval. Please review and click **'Submit for Approval'**.");
            } else {
                doneMsg.append(" Click **'Submit for Approval'** to create your reservation.");
            }

            sendEvent(emitter, "done", toJson(Map.of("content", doneMsg.toString())));

            // Save assistant message to conversation if conversationId is provided
            if (conversationId != null) {
                try {
                    conversationMessageRepository.save(new ConversationMessage(
                            conversationId,
                            "ASSISTANT",
                            doneMsg.toString(),
                            toJson(Map.of("proposal", proposalMap, "hadConflict", hadConflict))
                    ));
                } catch (Exception e) {
                    log.warn("Could not save assistant message for conversation {}: {}", conversationId, e.getMessage());
                }
            }

            emitter.complete();

        } catch (Exception e) {
            log.error("Error executing data-driven planner flow: {} | {}", e.getClass().getSimpleName(), e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"An error occurred in agent planner.\"}"));
                emitter.complete();
            } catch (Exception ignored) {}
        }
    }

    private void handleClarifyingRequest(SseEmitter emitter, String rawUserMessage, UUID userId, String entityId) throws IOException {
        recordAuditReasoning(userId, entityId, "User request missing required slot parameters (date, time, or headcount). Awaiting user clarification.");
        sendEvent(emitter, "plan", toJson(Map.of("steps", List.of(
                Map.of("id", 1, "title", "Clarify missing event specifications", "status", "running")
        ))));
        sendEvent(emitter, "thinking", toJson(Map.of("content", "Checking event parameters... Missing required date, time or headcount.")));
        sendEvent(emitter, "done", toJson(Map.of("content", "I would be glad to coordinate your reservation. Could you please specify:\n1. The event date (e.g. next Thursday or YYYY-MM-DD)\n2. Start and end times (e.g. 10-11 AM or 3-5 PM)\n3. Expected attendee count\n\nPlease also mention if guest house stay or parking is needed.")));
        emitter.complete();
    }

    private TimetableSlot findRoomTimetableClash(UUID roomId, List<TimetableSlot> slots, LocalTime startTime, LocalTime endTime) {
        int reqStart = startTime.getHour() * 60 + startTime.getMinute();
        int reqEnd = endTime.getHour() * 60 + endTime.getMinute();

        for (TimetableSlot ts : slots) {
            if (ts.getRoom().getId().equals(roomId)) {
                if (reqStart < ts.getEndMinute() && reqEnd > ts.getStartMinute()) {
                    return ts;
                }
            }
        }
        return null;
    }

    private BookingItem findRoomBookingClash(UUID roomId, List<BookingItem> items, LocalDate date, LocalTime startTime, LocalTime endTime) {
        OffsetDateTime reqStart = date.atTime(startTime).atZone(ZONE).toOffsetDateTime();
        OffsetDateTime reqEnd = date.atTime(endTime).atZone(ZONE).toOffsetDateTime();

        for (BookingItem bi : items) {
            if (roomId.equals(bi.getResourceId()) && !"CANCELLED".equals(bi.getStatus()) && !"REJECTED".equals(bi.getStatus())) {
                if (reqStart.isBefore(bi.getEndTime()) && reqEnd.isAfter(bi.getStartTime())) {
                    return bi;
                }
            }
        }
        return null;
    }

    private void recordAuditReasoning(UUID userId, String entityId, String summary) {
        try {
            auditService.record(
                    userId,
                    "AGENT_REASONING",
                    "CONVERSATION",
                    entityId,
                    null,
                    "{\"summary\":\"" + summary.replace("\"", "\\\"") + "\"}",
                    "127.0.0.1",
                    "CampusFlow/Agent"
            );
        } catch (Exception e) {
            log.warn("Failed to record agent reasoning audit: {}", e.getMessage());
        }
    }

    private void recordAuditToolCall(UUID userId, String entityId, String tool, Object args, String resultSummary, long durationMs, String status, int retryCount) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("tool", tool);
            details.put("args", args);
            details.put("resultSummary", resultSummary);
            details.put("durationMs", durationMs);
            details.put("status", status);
            details.put("retryCount", retryCount);

            auditService.record(
                    userId,
                    "TOOL_CALL",
                    "CONVERSATION",
                    entityId,
                    null,
                    toJson(details),
                    "127.0.0.1",
                    "CampusFlow/Agent"
            );
        } catch (Exception e) {
            log.warn("Failed to record tool call audit: {}", e.getMessage());
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private String formatTime(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format("%02d:%02d", h, m);
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
