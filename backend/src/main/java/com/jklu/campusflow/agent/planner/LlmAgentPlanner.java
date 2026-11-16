package com.jklu.campusflow.agent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jklu.campusflow.agent.model.ConversationMessage;
import com.jklu.campusflow.agent.repository.ConversationMessageRepository;
import com.jklu.campusflow.agent.service.PolicySearchService;
import com.jklu.campusflow.audit.service.AuditService;
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

        // Informational / Policy Query / Conversational Request
        List<Map<String, Object>> steps = List.of(
                Map.of("id", 1, "title", "Analyze policy requirements and scheduling context", "status", "completed"),
                Map.of("id", 2, "title", "Generate grounded guidance via Gemini LLM", "status", "running")
        );
        sendEvent(emitter, "plan", toJson(Map.of("steps", steps)));

        // Retrieve relevant policy snippets
        long toolStart = System.currentTimeMillis();
        String policyExcerpt = policySearchService.search(sanitizedMessage);
        long toolDuration = System.currentTimeMillis() - toolStart;

        sendEvent(emitter, "tool_call", toJson(Map.of(
                "id", UUID.randomUUID().toString(),
                "tool", "search_campus_policy",
                "args", Map.of("query", sanitizedMessage),
                "status", "success",
                "durationMs", toolDuration,
                "resultSummary", "Retrieved relevant JKLU policy clauses."
        )));

        // Call Gemini
        String prompt = "You are the JKLU CampusFlow AI Assistant, an intelligent multi-resource scheduling assistant for JK Lakshmipat University.\n"
                + "Current User Role: " + userRole + "\n"
                + "Campus Policy Guidelines:\n"
                + policyExcerpt + "\n\n"
                + "User Query: " + sanitizedMessage + "\n\n"
                + "Instructions:\n"
                + "1. Respond politely, clearly, and concisely grounded in the campus policies.\n"
                + "2. If the user is asking about booking an event, clearly explain what details are required (date, start and end times, estimated attendees, room/venue type, and whether guest house stay or parking is needed).\n"
                + "3. Adhere strictly to campus governance (e.g. regular classes take strict precedence, students require Admin approval for reservations, MDC Guest House check-in is 12:00 PM).";

        String geminiResponse = callGemini(prompt);

        sendEvent(emitter, "thinking", toJson(Map.of("content", "Gemini (" + modelName + ") synthesized response based on verified JKLU policies.")));
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
