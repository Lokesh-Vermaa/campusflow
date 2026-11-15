package com.jklu.campusflow.agent.controller;

import com.jklu.campusflow.agent.dto.AgentChatRequest;
import com.jklu.campusflow.agent.planner.LlmAgentPlanner;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.error.RateLimitExceededException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import com.jklu.campusflow.common.security.RequestIdFilter;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/agent")
@PreAuthorize("isAuthenticated()")
public class AgentController {

    private static final int MAX_AGENT_REQUESTS_PER_MINUTE = 10;
    private static final long SSE_TIMEOUT_MS = 30_000L;
    private static final ExecutorService SSE_POOL = Executors.newFixedThreadPool(5);

    private final Map<UUID, Deque<Instant>> userRateLimits = new ConcurrentHashMap<>();
    private final LlmAgentPlanner llmAgentPlanner;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public AgentController(LlmAgentPlanner llmAgentPlanner, UserRepository userRepository, AuditService auditService) {
        this.llmAgentPlanner = llmAgentPlanner;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @Valid @RequestBody AgentChatRequest request,
            Authentication auth
    ) {
        User user = resolveUser(auth);

        // Per-user rate limiting: 10 requests / minute
        checkRateLimit(user.getId());

        String userRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("STUDENT");

        // Write initial audit log
        auditService.record(
                user.getId(),
                "AGENT_CHAT",
                "AGENT",
                request.conversationId() != null ? request.conversationId().toString() : "new",
                null,
                "{\"messageLength\":" + request.message().length() + ",\"role\":\"" + userRole + "\"}",
                "127.0.0.1",
                "Agent/SSE"
        );

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String message = request.message(); // Treated strictly as DATA

        SSE_POOL.submit(() -> {
            try {
                llmAgentPlanner.plan(emitter, message, userRole, user.getId());
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"An unexpected error occurred during agent planning.\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    private void checkRateLimit(UUID userId) {
        Deque<Instant> timestamps = userRateLimits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant threshold = Instant.now().minusSeconds(60);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_AGENT_REQUESTS_PER_MINUTE) {
                throw new RateLimitExceededException("Agent chat rate limit exceeded. Max 10 messages per minute.", 60);
            }
            timestamps.addLast(Instant.now());
        }
    }

    private User resolveUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
