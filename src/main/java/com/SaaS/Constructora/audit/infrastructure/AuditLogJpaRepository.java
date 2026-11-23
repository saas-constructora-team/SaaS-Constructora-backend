package com.SaaS.Constructora.audit.infrastructure;

import com.SaaS.Constructora.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

interface AuditLogJpaRepository extends JpaRepository<AuditLog, UUID> {

    @Query(value = """
            select a from AuditLog a
            where a.tenantId = :tenantId
              and (:eventType is null or a.eventType = :eventType)
              and (:entityId is null or a.entityId = :entityId)
              and (:actorUserId is null or a.actorUserId = :actorUserId)
              and a.occurredAt >= coalesce(:from, a.occurredAt)
              and a.occurredAt <= coalesce(:to, a.occurredAt)
            """,
            countQuery = """
            select count(a) from AuditLog a
            where a.tenantId = :tenantId
              and (:eventType is null or a.eventType = :eventType)
              and (:entityId is null or a.entityId = :entityId)
              and (:actorUserId is null or a.actorUserId = :actorUserId)
              and a.occurredAt >= coalesce(:from, a.occurredAt)
              and a.occurredAt <= coalesce(:to, a.occurredAt)
            """)
    Page<AuditLog> search(
            @Param("tenantId") UUID tenantId,
            @Param("eventType") String eventType,
            @Param("entityId") UUID entityId,
            @Param("actorUserId") UUID actorUserId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
