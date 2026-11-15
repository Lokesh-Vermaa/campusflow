package com.jklu.campusflow.agent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationDto(UUID id, String title, UUID userId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
