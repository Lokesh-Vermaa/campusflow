package com.jklu.campusflow.agent.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private String role; // USER, ASSISTANT, SYSTEM, TOOL

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ConversationMessage() {}
    public ConversationMessage(UUID sessionId, String role, String content, String metadata) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.metadata = metadata;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getMetadata() { return metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
