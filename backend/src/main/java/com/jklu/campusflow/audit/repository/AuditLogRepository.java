package com.jklu.campusflow.audit.repository;

import com.jklu.campusflow.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:userId IS NULL OR a.userId = :userId)
             AND (:action IS NULL OR a.action = :action)
             AND (cast(:startDate as timestamp) IS NULL OR a.createdAt >= :startDate)
             AND (cast(:endDate as timestamp) IS NULL OR a.createdAt <= :endDate)
           ORDER BY a.createdAt DESC
           """)
    Page<AuditLog> findWithFilters(
            @Param("userId") UUID userId,
            @Param("action") String action,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );

    @Query("""
           SELECT a FROM AuditLog a
           WHERE (:userId IS NULL OR a.userId = :userId)
             AND (:action IS NULL OR a.action = :action)
             AND (cast(:startDate as timestamp) IS NULL OR a.createdAt >= :startDate)
             AND (cast(:endDate as timestamp) IS NULL OR a.createdAt <= :endDate)
           ORDER BY a.createdAt DESC
           """)
    List<AuditLog> findForExport(
            @Param("userId") UUID userId,
            @Param("action") String action,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
