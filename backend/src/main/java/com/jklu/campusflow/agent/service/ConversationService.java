package com.jklu.campusflow.agent.service;

import com.jklu.campusflow.agent.dto.ConversationDto;
import com.jklu.campusflow.agent.dto.ConversationMessageDto;
import com.jklu.campusflow.agent.model.ConversationMessage;
import com.jklu.campusflow.agent.model.ConversationSession;
import com.jklu.campusflow.agent.repository.ConversationMessageRepository;
import com.jklu.campusflow.agent.repository.ConversationSessionRepository;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConversationService {

    private final ConversationSessionRepository sessionRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationSessionRepository sessionRepository,
            ConversationMessageRepository messageRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<ConversationDto> listConversations(UUID userId, int page, int pageSize) {
        int safePage = Math.max(0, page - 1);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Page<ConversationSession> result = sessionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(safePage, safeSize));
        return new PaginatedResponse<>(
                result.getContent().stream().map(this::toDto).toList(),
                page, safeSize, result.getTotalElements(), result.getTotalPages()
        );
    }

    public ConversationDto createConversation(UUID userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        ConversationSession session = sessionRepository.save(new ConversationSession(user, title));
        return toDto(session);
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversation(UUID conversationId, UUID userId) {
        // IDOR: same 404 for non-existent and other-user's conversations
        ConversationSession session = sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return toDto(session);
    }

    public ConversationDto renameConversation(UUID conversationId, UUID userId, String newTitle) {
        ConversationSession session = sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        session.setTitle(newTitle);
        return toDto(sessionRepository.save(session));
    }

    public void deleteConversation(UUID conversationId, UUID userId) {
        ConversationSession session = sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageDto> getMessages(UUID conversationId, UUID userId) {
        // IDOR check
        sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(conversationId).stream()
                .map(m -> new ConversationMessageDto(
                        m.getId(), m.getSessionId(), m.getRole(), m.getContent(), m.getMetadata(), m.getCreatedAt()
                ))
                .toList();
    }

    public ConversationDto retry(UUID conversationId, UUID userId) {
        // IDOR check
        ConversationSession session = sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return toDto(session);
    }

    public void stop(UUID conversationId, UUID userId) {
        // IDOR check
        sessionRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }

    public void addMessage(UUID conversationId, String role, String content, String metadata) {
        messageRepository.save(new ConversationMessage(conversationId, role, content, metadata));
    }

    private ConversationDto toDto(ConversationSession s) {
        return new ConversationDto(s.getId(), s.getTitle(), s.getUser().getId(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
