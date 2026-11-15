package com.jklu.campusflow.agent.controller;

import com.jklu.campusflow.agent.dto.ConversationDto;
import com.jklu.campusflow.agent.dto.ConversationMessageDto;
import com.jklu.campusflow.agent.dto.RenameConversationRequest;
import com.jklu.campusflow.agent.service.ConversationService;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@PreAuthorize("isAuthenticated()")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(ConversationService conversationService, UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
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

    @PostMapping("/{id}/retry")
    public ResponseEntity<ConversationDto> retry(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(conversationService.retry(id, resolveUserId(auth)));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, String>> stop(@PathVariable UUID id, Authentication auth) {
        conversationService.stop(id, resolveUserId(auth));
        return ResponseEntity.ok(Map.of("status", "STOPPED", "message", "Conversation generation stopped"));
    }

    private UUID resolveUserId(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
}
