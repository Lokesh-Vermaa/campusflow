package com.jklu.campusflow.agent.controller;

import com.jklu.campusflow.agent.dto.AgentChatRequest;
import com.jklu.campusflow.agent.planner.LlmAgentPlanner;
import com.jklu.campusflow.agent.service.ConversationService;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.error.RateLimitExceededException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
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

    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final ExecutorService SSE_POOL = Executors.newFixedThreadPool(10);

    private final Map<UUID, Deque<Instant>> userRateLimits = new ConcurrentHashMap<>();
    private final LlmAgentPlanner llmAgentPlanner;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final ConversationService conversationService;

    @org.springframework.beans.factory.annotation.Value("${agent.executor.async:true}")
    private boolean asyncExecution = true;

    @org.springframework.beans.factory.annotation.Value("${agent.rate-limit.per-minute:10}")
    private int maxRequestsPerMinute = 10;

    public AgentController(
            LlmAgentPlanner llmAgentPlanner,
            UserRepository userRepository,
            AuditService auditService,
            ConversationService conversationService
    ) {
        this.llmAgentPlanner = llmAgentPlanner;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @Valid @RequestBody AgentChatRequest request,
            Authentication auth
    ) {
        User user = resolveUser(auth);

        // Per-user rate limiting: 10 requests / minute
        checkRateLimit(user.getId());

        // IDOR check: if conversationId is provided, caller must own it or get 404
        if (request.conversationId() != null) {
            conversationService.getConversation(request.conversationId(), user.getId());
        }

        String userRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("STUDENT");

        // Write initial audit log
        auditService.record(
                user.getId(),
                "AGENT_CHAT",
                "CONVERSATION",
                request.conversationId() != null ? request.conversationId().toString() : "new",
                null,
                "{\"messageLength\":" + request.message().length() + ",\"role\":\"" + userRole + "\"}",
                "127.0.0.1",
                "Agent/SSE"
        );

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String message = request.message();
        UUID convId = request.conversationId();

        Runnable task = () -> {
            try {
                llmAgentPlanner.plan(emitter, message, userRole, user.getId(), convId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(AgentController.class).error("Agent planning error: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"An unexpected error occurred during agent planning.\"}"));
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        };

        if (asyncExecution) {
            SSE_POOL.submit(task);
        } else {
            task.run();
        }

        return emitter;
    }

    private void checkRateLimit(UUID userId) {
        Deque<Instant> timestamps = userRateLimits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant threshold = Instant.now().minusSeconds(60);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequestsPerMinute) {
                throw new RateLimitExceededException("Agent chat rate limit exceeded. Max " + maxRequestsPerMinute + " messages per minute.", 60);
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
