package com.jklu.campusflow.audit.service;

import com.jklu.campusflow.audit.dto.AuditLogResponse;
import com.jklu.campusflow.audit.model.AuditLog;
import com.jklu.campusflow.audit.repository.AuditLogRepository;
import com.jklu.campusflow.auth.User;
import com.jklu.campusflow.auth.UserRepository;
import com.jklu.campusflow.common.dto.PaginatedResponse;
import com.jklu.campusflow.timetable.parser.TimetableParser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final Map<UUID, User> userCache = new java.util.concurrent.ConcurrentHashMap<>();

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @jakarta.annotation.PostConstruct
    public void initUserCache() {
        try {
            userRepository.findAll().forEach(u -> userCache.put(u.getId(), u));
        } catch (Exception ignored) {}
    }

    @Transactional
    public void record(UUID userId, String action, String entityType, String entityId, String oldState, String newState, String ipAddress, String userAgent) {
        AuditLog entry = new AuditLog(userId, action, entityType, entityId, oldState, newState, ipAddress, userAgent);
        auditLogRepository.save(entry);
    }

    public PaginatedResponse<AuditLogResponse> searchAuditLogs(
            String actor,
            String eventType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(0, page - 1);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        Pageable pageable = PageRequest.of(safePage, safePageSize);

        UUID actorId = resolveActorId(actor);
        Page<AuditLog> logPage = auditLogRepository.findWithFilters(actorId, eventType, startDate, endDate, pageable);

        Set<UUID> missingUserIds = logPage.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .filter(id -> !userCache.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingUserIds.isEmpty()) {
            userRepository.findAllById(missingUserIds).forEach(u -> userCache.put(u.getId(), u));
        }

        List<AuditLogResponse> items = logPage.getContent().stream()
                .map(a -> toResponse(a, userCache))
                .toList();

        return new PaginatedResponse<>(items, page, safePageSize, logPage.getTotalElements(), logPage.getTotalPages());
    }

    public String exportToCsv(String actor, String eventType, OffsetDateTime startDate, OffsetDateTime endDate) {
        UUID actorId = resolveActorId(actor);
        List<AuditLog> entries = auditLogRepository.findForExport(actorId, eventType, startDate, endDate);

        StringWriter sw = new StringWriter();
        sw.append("id,user_id,action,entity_type,entity_id,ip_address,created_at\n");

        for (AuditLog a : entries) {
            sw.append(escapeCsv(a.getId().toString())).append(",")
              .append(escapeCsv(a.getUserId() != null ? a.getUserId().toString() : "")).append(",")
              .append(escapeCsv(a.getAction())).append(",")
              .append(escapeCsv(a.getEntityType())).append(",")
              .append(escapeCsv(a.getEntityId())).append(",")
              .append(escapeCsv(a.getIpAddress() != null ? a.getIpAddress() : "")).append(",")
              .append(escapeCsv(a.getCreatedAt().toString())).append("\n");
        }

        return sw.toString();
    }

    private UUID resolveActorId(String actor) {
        if (actor == null || actor.isBlank()) return null;
        try {
            return UUID.fromString(actor.trim());
        } catch (IllegalArgumentException e) {
            String lower = actor.trim().toLowerCase();
            return userRepository.findAll().stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(lower) || u.getEmail().toLowerCase().contains(lower))
                    .map(User::getId)
                    .findFirst()
                    .orElse(UUID.randomUUID());
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        // Neutralize CSV formula injection (=, +, -, @)
        String safe = TimetableParser.sanitizeFormula(val);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private AuditLogResponse toResponse(AuditLog a, Map<UUID, User> userMap) {
        User u = a.getUserId() != null ? userMap.get(a.getUserId()) : null;
        String actorName = u != null ? u.getFullName() : (a.getUserId() != null ? "User " + a.getUserId().toString().substring(0, 8) : "System");
        String actorRole = u != null ? u.getRole() : "SYSTEM";
        String eventType = a.getAction() != null ? a.getAction() : "DATA_CHANGE";

        return new AuditLogResponse(
                a.getId(),
                a.getUserId(),
                actorName,
                actorRole,
                eventType,
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getOldState(),
                a.getNewState(),
                a.getIpAddress(),
                a.getCreatedAt()
        );
    }
}
