package com.jklu.campusflow.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        String actor,
        String actorRole,
        String eventType,
        String action,
        String entityType,
        String entityId,
        String oldState,
        String newState,
        String ipAddress,
        OffsetDateTime createdAt
) {}
