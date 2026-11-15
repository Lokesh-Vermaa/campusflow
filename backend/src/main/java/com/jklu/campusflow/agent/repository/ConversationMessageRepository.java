package com.jklu.campusflow.agent.repository;

import com.jklu.campusflow.agent.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
