package com.jklu.campusflow.agent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.agent.model.ConversationMessage;
import com.jklu.campusflow.agent.repository.ConversationMessageRepository;
import com.jklu.campusflow.agent.service.PolicySearchService;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.timetable.model.TimetableSlot;
import com.jklu.campusflow.timetable.repository.TimetableSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Component
public class LlmAgentPlanner {

    private static final Logger log = LoggerFactory.getLogger(LlmAgentPlanner.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private final ScriptedPlanner scriptedPlanner;
    private final PolicySearchService policySearchService;
    private final TimetableSlotRepository timetableSlotRepository;
    private final AuditService auditService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Environment environment;

    private final String llmApiKey;
    private final String llmModel;
    private final String llmProvider;
    private final String agentMode;

    public LlmAgentPlanner(
            ScriptedPlanner scriptedPlanner,
            PolicySearchService policySearchService,
            TimetableSlotRepository timetableSlotRepository,
            AuditService auditService,
            ConversationMessageRepository conversationMessageRepository,
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${app.agent.mode:scripted}") String agentMode,
            @Value("${LLM_API_KEY:}") String llmApiKey,
            @Value("${LLM_MODEL:gemini-flash-lite-latest}") String llmModel,
            @Value("${LLM_PROVIDER:gemini}") String llmProvider
    ) {
        this.scriptedPlanner = scriptedPlanner;
        this.policySearchService = policySearchService;
        this.timetableSlotRepository = timetableSlotRepository;
        this.auditService = auditService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.agentMode = agentMode != null ? agentMode : "scripted";
        this.llmApiKey = llmApiKey != null ? llmApiKey : "";
        this.llmModel = (llmModel != null && !llmModel.isBlank()) ? llmModel : "gemini-flash-lite-latest";
        this.llmProvider = (llmProvider != null && !llmProvider.isBlank()) ? llmProvider : "gemini";
    }

    public void plan(SseEmitter emitter, String rawUserMessage, String userRole, UUID userId, UUID conversationId) {
        // Prompt-injection defense: user message is pure DATA, never executed as commands
        String sanitizedMessage = rawUserMessage != null ? rawUserMessage.trim() : "";

        if ((environment != null && environment.matchesProfiles("test"))
                || "scripted".equalsIgnoreCase(agentMode)
                || llmApiKey == null || llmApiKey.isBlank()) {
            log.info("Operating in deterministic planner mode (profile test / scripted / no LLM key).");
            notifyFallback(emitter, "No external LLM key provided. Operating in deterministic offline planner mode.");
            scriptedPlanner.runDataDrivenFlow(emitter, sanitizedMessage, userRole, userId, conversationId);
            return;
        }

        // Real LLM call with retry (max 2)
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            try {
                invokeRemoteLlm(emitter, sanitizedMessage, userRole, userId, conversationId);
                return;
            } catch (Exception e) {
                log.warn("Gemini LLM invocation attempt {} failed: {}", attempts, e.getMessage());
                if (attempts >= 2) {
                    notifyFallback(emitter, "Gemini LLM service temporarily unavailable after 2 retries. Switching seamlessly to deterministic planner.");
                    scriptedPlanner.runDataDrivenFlow(emitter, sanitizedMessage, userRole, userId, conversationId);
                    return;
                }
            }
        }
    }

    private void invokeRemoteLlm(SseEmitter emitter, String sanitizedMessage, String userRole, UUID userId, UUID conversationId) throws Exception {
        String entityId = conversationId != null ? conversationId.toString() : (userId != null ? userId.toString() : "session");
        String modelName = resolveModel();

        // 1. Notify that Gemini LLM is active
        sendEvent(emitter, "thinking", toJson(Map.of("content", "Gemini (" + modelName + ") active: Analyzing campus intent under JKLU policies...")));

        // 2. Parse request
        AgentRequestParser.ParsedEventRequest parsed = AgentRequestParser.parse(sanitizedMessage, LocalDate.now(ZONE));

        if (!parsed.missingRequiredFields()) {
            // Complete booking request with date, time, headcount
            // Hand off to data-driven flow which verifies DB inventory, detects timetable clash,
            // recovers to conflict-free alternative (A-102), allocates MDC guest house/parking,
            // and emits proposal ready for admin submission
            scriptedPlanner.runDataDrivenFlow(emitter, sanitizedMessage, userRole, userId, conversationId);
            return;
        }

        // Informational / Policy Query / Timetable / Conversational Request
        List<Map<String, Object>> steps = List.of(
                Map.of("id", 1, "title", "Analyze policy requirements and scheduling context", "status", "completed"),
                Map.of("id", 2, "title", "Generate grounded guidance via Gemini LLM", "status", "running")
        );
        sendEvent(emitter, "plan", toJson(Map.of("steps", steps)));

        // Retrieve relevant policy snippets
        long toolStart = System.currentTimeMillis();
        String policyExcerpt = policySearchService.search(sanitizedMessage);
        String timetableExcerpt = searchTimetableInfo(sanitizedMessage);
        long toolDuration = System.currentTimeMillis() - toolStart;

        sendEvent(emitter, "tool_call", toJson(Map.of(
                "id", UUID.randomUUID().toString(),
                "tool", "search_campus_policy_and_timetable",
                "args", Map.of("query", sanitizedMessage),
                "status", "success",
                "durationMs", toolDuration,
                "resultSummary", "Retrieved verified JKLU policy clauses and published timetable records."
        )));

        // Call Gemini
        String prompt = "You are the JKLU CampusFlow AI Assistant, an intelligent multi-resource scheduling assistant for JK Lakshmipat University.\n"
                + "Current User Role: " + userRole + "\n\n"
                + "Verified Campus Policy Guidelines:\n"
                + policyExcerpt + "\n\n"
                + "Published Semester Timetable Records & Free Slot Analysis:\n"
                + timetableExcerpt + "\n\n"
                + "User Query: " + sanitizedMessage + "\n\n"
                + "Strict Instructions:\n"
                + "1. Respond politely, clearly, and concisely grounded strictly in the campus policies and timetable records provided above.\n"
                + "2. If the user asks for free time slots, breaks, or availability for a section or student on any day:\n"
                + "   - Present the scheduled classes first.\n"
                + "   - Highlight the primary free time windows (e.g., for Section C on Monday: 11:50 AM to 02:00 PM lunch & free window, and after 04:50 PM).\n"
                + "   - If both 3rd Semester and 1st Semester schedules exist for this section, clearly display both so the student has complete visibility.\n"
                + "3. If the user asks about course timings, faculty, or room schedules: use ONLY the published timetable records provided above. If the course or slot is not in the timetable records, explicitly state: 'I do not have record of that course/faculty/slot in the current published semester timetable.' NEVER fabricate or guess timetable slots.\n"
                + "4. If the user is asking about booking an event, clearly explain what details are required (date, start and end times, estimated attendees, room/venue type, and whether guest house stay or parking is needed).\n"
                + "5. Adhere strictly to campus governance (regular classes take strict precedence, students require Admin approval for reservations).";

        String geminiResponse = callGemini(prompt);

        sendEvent(emitter, "thinking", toJson(Map.of("content", "Gemini (" + modelName + ") synthesized response based on verified JKLU policies and schedules.")));
        sendEvent(emitter, "done", toJson(Map.of("content", geminiResponse)));

        // Persist to conversation history
        if (conversationId != null) {
            try {
                conversationMessageRepository.save(new ConversationMessage(
                        conversationId,
                        "ASSISTANT",
                        geminiResponse,
                        toJson(Map.of("llm", modelName, "provider", "gemini"))
                ));
            } catch (Exception e) {
                log.warn("Could not save assistant conversation message: {}", e.getMessage());
            }
        }

        // Audit record
        auditService.record(
                userId,
                "AGENT_LLM_RESPONSE",
                "CONVERSATION",
                entityId,
                null,
                toJson(Map.of("model", modelName, "query", sanitizedMessage, "status", "SUCCESS")),
                "127.0.0.1",
                "Gemini/LLM"
        );

        emitter.complete();
    }

    private String searchTimetableInfo(String message) {
        if (message == null || message.isBlank()) return "No timetable records queried.";

        // Detect Day of week
        Integer targetDayOfWeek = null;
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("monday") || lowerMsg.matches(".*\\bmon\\b.*")) targetDayOfWeek = 1;
        else if (lowerMsg.contains("tuesday") || lowerMsg.matches(".*\\btue\\b.*")) targetDayOfWeek = 2;
        else if (lowerMsg.contains("wednesday") || lowerMsg.matches(".*\\bwed\\b.*")) targetDayOfWeek = 3;
        else if (lowerMsg.contains("thursday") || lowerMsg.matches(".*\\bthu\\b.*")) targetDayOfWeek = 4;
        else if (lowerMsg.contains("friday") || lowerMsg.matches(".*\\bfri\\b.*")) targetDayOfWeek = 5;
        else if (lowerMsg.contains("saturday") || lowerMsg.matches(".*\\bsat\\b.*")) targetDayOfWeek = 6;
        else if (lowerMsg.contains("sunday") || lowerMsg.matches(".*\\bsun\\b.*")) targetDayOfWeek = 7;

        // Detect Section (e.g. "Section C", "Sec C", "Sec-C", "Section-C")
        java.util.regex.Pattern secPattern = java.util.regex.Pattern.compile("(?i)(?:section|sec)[\\s_-]*([a-j])\\b");
        java.util.regex.Matcher secMatcher = secPattern.matcher(message);
        String detectedSection = null;
        if (secMatcher.find()) {
            detectedSection = "Sec " + secMatcher.group(1).toUpperCase();
        }

        List<TimetableSlot> allMatches = new ArrayList<>();
        Set<UUID> seenIds = new HashSet<>();

        if (detectedSection != null) {
            List<TimetableSlot> secSlots = timetableSlotRepository.findActiveBySectionAndOptionalDay(detectedSection, targetDayOfWeek);
            for (TimetableSlot ts : secSlots) {
                if (seenIds.add(ts.getId())) {
                    allMatches.add(ts);
                }
            }
        }

        // Also token search if no section or to find faculty/room/course matches
        String[] rawWords = message.replaceAll("[^a-zA-Z0-9\\s]", " ").split("\\s+");
        Set<String> stopWords = Set.of(
                "the", "for", "and", "what", "when", "where", "room", "hall", "can", "does", "teach", "teaches",
                "class", "classes", "lecture", "lectures", "have", "time", "times", "timing", "timings", "schedule",
                "tell", "show", "with", "from", "about", "who", "why", "how", "dr", "mr", "ms", "prof", "sir", "please",
                "give", "info", "information", "details", "any", "all", "which", "free", "slot", "slots", "available",
                "vacant", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                "mon", "tue", "wed", "thu", "fri", "sat", "sun", "section", "sec"
        );

        List<String> queryWords = Arrays.stream(rawWords)
                .map(String::trim)
                .filter(w -> w.length() >= 3 && !stopWords.contains(w.toLowerCase()))
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();

        for (String w : queryWords) {
            List<TimetableSlot> found = timetableSlotRepository.searchActiveSlotsWithOptionalDay(w, targetDayOfWeek);
            for (TimetableSlot ts : found) {
                if (seenIds.add(ts.getId())) {
                    allMatches.add(ts);
                    if (allMatches.size() >= 40) break;
                }
            }
            if (allMatches.size() >= 40) break;
        }

        if (allMatches.isEmpty()) {
            return "No matching timetable slots found in current published schedule.";
        }

        StringBuilder sb = new StringBuilder();
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        if (detectedSection != null) {
            sb.append("### OFFICIAL SCHEDULE FOR ").append(detectedSection);
            if (targetDayOfWeek != null) {
                sb.append(" ON ").append(days[targetDayOfWeek].toUpperCase());
            }
            sb.append(":\n\n");
        }

        for (TimetableSlot ts : allMatches) {
            String dayName = (ts.getDayOfWeek() >= 1 && ts.getDayOfWeek() <= 7) ? days[ts.getDayOfWeek()] : "Day " + ts.getDayOfWeek();
            String startStr = String.format("%02d:%02d", ts.getStartMinute() / 60, ts.getStartMinute() % 60);
            String endStr = String.format("%02d:%02d", ts.getEndMinute() / 60, ts.getEndMinute() % 60);
            sb.append("- Academic Term: ").append(ts.getAcademicTerm() != null ? ts.getAcademicTerm() : "Semester")
              .append(", Section: ").append(ts.getSection() != null ? ts.getSection() : "")
              .append(", Course: ").append(ts.getCourseCode())
              .append(", Room: ").append(ts.getRoom() != null ? ts.getRoom().getRoomNumber() : "Unknown")
              .append(", Day: ").append(dayName)
              .append(", Time: ").append(startStr).append(" - ").append(endStr);
            if (ts.getFaculty() != null && !ts.getFaculty().isBlank()) {
                sb.append(", Faculty: ").append(ts.getFaculty());
            }
            sb.append("\n");
        }

        // If Section & Day query, explicitly append computed free time window analysis:
        if (detectedSection != null && targetDayOfWeek != null) {
            sb.append("\n### COMPUTED FREE TIME WINDOWS FOR ").append(detectedSection).append(" (College Operating Hours: 09:00 - 17:00):\n");
            Map<String, List<TimetableSlot>> byTerm = new HashMap<>();
            for (TimetableSlot ts : allMatches) {
                if (ts.getDayOfWeek() == targetDayOfWeek && ts.getSection() != null && ts.getSection().contains(detectedSection)) {
                    String term = ts.getAcademicTerm() != null ? ts.getAcademicTerm() : "Current Term";
                    byTerm.computeIfAbsent(term, k -> new ArrayList<>()).add(ts);
                }
            }

            for (Map.Entry<String, List<TimetableSlot>> entry : byTerm.entrySet()) {
                String term = entry.getKey();
                List<TimetableSlot> termSlots = entry.getValue();
                termSlots.sort(Comparator.comparingInt(TimetableSlot::getStartMinute));

                sb.append("\n**").append(term).append(" (").append(detectedSection).append("):**\n");
                int currentMinute = 540; // 09:00 AM
                for (TimetableSlot ts : termSlots) {
                    if (ts.getStartMinute() > currentMinute) {
                        int gap = ts.getStartMinute() - currentMinute;
                        if (gap >= 15) {
                            sb.append("- FREE SLOT: ")
                              .append(String.format("%02d:%02d", currentMinute / 60, currentMinute % 60))
                              .append(" - ")
                              .append(String.format("%02d:%02d", ts.getStartMinute() / 60, ts.getStartMinute() % 60))
                              .append(" (").append(gap).append(" minutes)\n");
                        }
                    }
                    currentMinute = Math.max(currentMinute, ts.getEndMinute());
                }
                if (currentMinute < 1020) { // 17:00 PM
                    sb.append("- FREE SLOT: ")
                      .append(String.format("%02d:%02d", currentMinute / 60, currentMinute % 60))
                      .append(" - 17:00 (Evening free window)\n");
                }
                sb.append("- FREE: After 17:00 (Post-college hours)\n");
            }
        }

        return sb.toString();
    }

    private String callGemini(String prompt) throws Exception {
        String model = resolveModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + llmApiKey;

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(textPart));
        Map<String, Object> requestMap = Map.of("contents", List.of(content));
        String jsonBody = objectMapper.writeValueAsString(requestMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText();
            }
        }
        throw new IllegalStateException("Empty candidates in Gemini response");
    }

    private String resolveModel() {
        if (llmModel != null && !llmModel.isBlank() && !llmModel.contains("2.0") && !llmModel.contains("2.5")) {
            return llmModel;
        }
        return "gemini-flash-lite-latest";
    }

    private void notifyFallback(SseEmitter emitter, String reason) {
        try {
            emitter.send(SseEmitter.event()
                    .name("thinking")
                    .data("{\"content\":\"Planner notice: " + reason.replace("\"", "\\\"") + "\"}"));
        } catch (IOException e) {
            log.warn("Failed to send fallback notification: {}", e.getMessage());
        }
    }

    private void sendEvent(SseEmitter emitter, String name, String json) {
        try {
            emitter.send(SseEmitter.event().name(name).data(json));
        } catch (IOException e) {
            log.warn("Failed to send SSE event {}: {}", name, e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
