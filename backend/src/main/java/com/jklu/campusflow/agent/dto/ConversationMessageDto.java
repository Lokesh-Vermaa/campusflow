package com.jklu.campusflow.agent.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationMessageDto(
        UUID id,
        UUID sessionId,
        String role,
        String content,
        String metadata,
        OffsetDateTime createdAt
) {}
