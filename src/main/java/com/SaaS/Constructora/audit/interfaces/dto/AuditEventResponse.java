package com.SaaS.Constructora.audit.interfaces.dto;

import com.SaaS.Constructora.audit.domain.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditEventResponse(
        UUID eventId,
        UUID tenantId,
        String eventType,
        String entityType,
        UUID entityId,
        UUID actorUserId,
        LocalDateTime occurredAt,
        String summary
) {
    public static AuditEventResponse from(AuditLog entry) {
        return new AuditEventResponse(
                entry.getId(), entry.getTenantId(), entry.getEventType(), entry.getEntityType(),
                entry.getEntityId(), entry.getActorUserId(), entry.getOccurredAt(),
                entry.getSummary());
    }
}
