package com.jklu.campusflow.audit.service;

import com.jklu.campusflow.audit.dto.AuditLogResponse;
import com.jklu.campusflow.audit.model.AuditLog;
import com.jklu.campusflow.audit.repository.AuditLogRepository;
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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID userId, String action, String entityType, String entityId, String oldState, String newState, String ipAddress, String userAgent) {
        AuditLog entry = new AuditLog(userId, action, entityType, entityId, oldState, newState, ipAddress, userAgent);
        auditLogRepository.save(entry);
    }

    public PaginatedResponse<AuditLogResponse> searchAuditLogs(
            UUID actor,
            String eventType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            int page,
            int pageSize
    ) {
        int safePage = Math.max(0, page - 1);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        Pageable pageable = PageRequest.of(safePage, safePageSize);

        Page<AuditLog> logPage = auditLogRepository.findWithFilters(actor, eventType, startDate, endDate, pageable);

        List<AuditLogResponse> items = logPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PaginatedResponse<>(items, page, safePageSize, logPage.getTotalElements(), logPage.getTotalPages());
    }

    public String exportToCsv(UUID actor, String eventType, OffsetDateTime startDate, OffsetDateTime endDate) {
        List<AuditLog> entries = auditLogRepository.findForExport(actor, eventType, startDate, endDate);

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

    private String escapeCsv(String val) {
        if (val == null) return "";
        // Neutralize CSV formula injection (=, +, -, @)
        String safe = TimetableParser.sanitizeFormula(val);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getUserId(),
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
