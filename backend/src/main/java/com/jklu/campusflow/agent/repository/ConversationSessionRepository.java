package com.jklu.campusflow.agent.repository;

import com.jklu.campusflow.agent.model.ConversationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ConversationSessionRepository extends JpaRepository<ConversationSession, UUID> {
    Page<ConversationSession> findAllByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
    Optional<ConversationSession> findByIdAndUserId(UUID id, UUID userId);
}
