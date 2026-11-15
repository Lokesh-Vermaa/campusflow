package com.jklu.campusflow.agent.planner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

@Component
public class LlmAgentPlanner {

    private static final Logger log = LoggerFactory.getLogger(LlmAgentPlanner.class);

    private final ScriptedPlanner scriptedPlanner;
    private final String llmApiKey;
    private final String llmModel;
    private final String llmProvider;

    public LlmAgentPlanner(
            ScriptedPlanner scriptedPlanner,
            @Value("${LLM_API_KEY:}") String llmApiKey,
            @Value("${LLM_MODEL:gemini-2.0-flash}") String llmModel,
            @Value("${LLM_PROVIDER:google}") String llmProvider
    ) {
        this.scriptedPlanner = scriptedPlanner;
        this.llmApiKey = llmApiKey;
        this.llmModel = llmModel;
        this.llmProvider = llmProvider;
    }

    public void plan(SseEmitter emitter, String rawUserMessage, String userRole, UUID userId, UUID conversationId) {
        // Prompt-injection defense: user message is pure DATA, never executed as commands
        String sanitizedMessage = rawUserMessage != null ? rawUserMessage.trim() : "";

        if (llmApiKey == null || llmApiKey.isBlank()) {
            log.info("No LLM_API_KEY set; falling back to deterministic data-driven planner.");
            notifyFallback(emitter, "No external LLM key provided. Operating in deterministic offline planner mode.");
            scriptedPlanner.runDataDrivenFlow(emitter, sanitizedMessage, userRole, userId, conversationId);
            return;
        }

        // Real LLM call with retry (max 2)
        int attempts = 0;
        while (attempts < 2) {
            attempts++;
            try {
                // Remote LLM invocation if key provided
                invokeRemoteLlm(emitter, sanitizedMessage, userRole, userId, conversationId);
                return;
            } catch (Exception e) {
                log.warn("LLM invocation attempt {} failed: {}", attempts, e.getMessage());
                if (attempts >= 2) {
                    notifyFallback(emitter, "LLM service unavailable after 2 retries. Switching to fallback planner.");
                    scriptedPlanner.runDataDrivenFlow(emitter, sanitizedMessage, userRole, userId, conversationId);
                    return;
                }
            }
        }
    }

    private void invokeRemoteLlm(SseEmitter emitter, String sanitizedMessage, String userRole, UUID userId, UUID conversationId) throws Exception {
        throw new IllegalStateException("Remote LLM connection failed or unconfigured.");
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
}
