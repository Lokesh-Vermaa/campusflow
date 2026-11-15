package com.jklu.campusflow.agent.controller;

import com.jklu.campusflow.agent.dto.AgentChatRequest;
import com.jklu.campusflow.agent.dto.ConversationDto;
import com.jklu.campusflow.agent.dto.ConversationMessageDto;
import com.jklu.campusflow.agent.dto.RenameConversationRequest;
import com.jklu.campusflow.agent.model.ConversationMessage;
import com.jklu.campusflow.agent.planner.LlmAgentPlanner;
import com.jklu.campusflow.agent.repository.ConversationMessageRepository;
import com.jklu.campusflow.agent.service.ConversationService;
import com.jklu.campusflow.audit.service.AuditService;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.RateLimitExceededException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/conversations")
@PreAuthorize("isAuthenticated()")
public class ConversationController {

    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final ExecutorService SSE_POOL = Executors.newFixedThreadPool(10);

    private final Map<UUID, Deque<Instant>> userRateLimits = new ConcurrentHashMap<>();
    private final ConversationService conversationService;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final LlmAgentPlanner llmAgentPlanner;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Value("${agent.executor.async:true}")
    private boolean asyncExecution = true;

    @org.springframework.beans.factory.annotation.Value("${agent.rate-limit.per-minute:10}")
    private int maxMessagesPerMinute = 10;

    public ConversationController(
            ConversationService conversationService,
            ConversationMessageRepository messageRepository,
            UserRepository userRepository,
            LlmAgentPlanner llmAgentPlanner,
            AuditService auditService
    ) {
        this.conversationService = conversationService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.llmAgentPlanner = llmAgentPlanner;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ConversationDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication auth) {
        return ResponseEntity.ok(conversationService.listConversations(resolveUserId(auth), page, pageSize));
    }

    @PostMapping
    public ResponseEntity<ConversationDto> create(
            @RequestBody(required = false) RenameConversationRequest req,
            Authentication auth) {
        String title = req != null && req.title() != null ? req.title() : "New Conversation";
        ConversationDto dto = conversationService.createConversation(resolveUserId(auth), title);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(conversationService.getConversation(id, resolveUserId(auth)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ConversationDto> rename(
            @PathVariable UUID id,
            @Valid @RequestBody RenameConversationRequest req,
            Authentication auth) {
        return ResponseEntity.ok(conversationService.renameConversation(id, resolveUserId(auth), req.title()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication auth) {
        conversationService.deleteConversation(id, resolveUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<List<ConversationMessageDto>> getMessages(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(conversationService.getMessages(id, resolveUserId(auth)));
    }

    @PostMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AgentChatRequest req,
            Authentication auth) {
        User user = resolveUser(auth);

        // Rate limiting check
        checkRateLimit(user.getId());

        // IDOR verification: conversation must belong to the caller (returns 404 otherwise)
        conversationService.getConversation(id, user.getId());

        // Save incoming user message
        messageRepository.save(new ConversationMessage(id, "USER", req.message(), "{}"));

        String userRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse("STUDENT");

        // Record audit entry for streaming chat initiated
        auditService.record(
                user.getId(),
                "AGENT_CHAT",
                "CONVERSATION",
                id.toString(),
                null,
                "{\"messageLength\":" + req.message().length() + ",\"role\":\"" + userRole + "\"}",
                "127.0.0.1",
                "Conversation/SSE"
        );

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        String message = req.message();

        Runnable task = () -> {
            try {
                llmAgentPlanner.plan(emitter, message, userRole, user.getId(), id);
            } catch (Exception e) {
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

    @PostMapping("/{id}/retry")
    public ResponseEntity<ConversationDto> retry(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(conversationService.retry(id, resolveUserId(auth)));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, String>> stop(@PathVariable UUID id, Authentication auth) {
        conversationService.stop(id, resolveUserId(auth));
        return ResponseEntity.ok(Map.of("status", "STOPPED", "message", "Conversation generation stopped"));
    }

    private void checkRateLimit(UUID userId) {
        Deque<Instant> timestamps = userRateLimits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant threshold = Instant.now().minusSeconds(60);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxMessagesPerMinute) {
                throw new RateLimitExceededException("Agent messages rate limit exceeded. Max " + maxMessagesPerMinute + " messages per minute.", 60);
            }
            timestamps.addLast(Instant.now());
        }
    }

    private UUID resolveUserId(Authentication auth) {
        return resolveUser(auth).getId();
    }

    private User resolveUser(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
