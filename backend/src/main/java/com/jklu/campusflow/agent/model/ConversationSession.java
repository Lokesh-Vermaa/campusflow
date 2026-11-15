package com.jklu.campusflow.agent.model;

import com.jklu.campusflow.auth.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_sessions")
public class ConversationSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title = "New Conversation";

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public ConversationSession() {}
    public ConversationSession(User user, String title) {
        this.user = user;
        this.title = title != null ? title : "New Conversation";
    }

    // Getters and setters
    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; this.updatedAt = OffsetDateTime.now(); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
