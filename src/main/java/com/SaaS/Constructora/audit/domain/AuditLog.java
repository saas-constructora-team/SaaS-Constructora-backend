package com.SaaS.Constructora.audit.domain;

import com.SaaS.Constructora.shared.events.MembershipRevoked;
import com.SaaS.Constructora.shared.events.ProjectAssignmentChanged;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import com.SaaS.Constructora.shared.events.ProjectStageClosed;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/** Entrada del historial de auditoría. El eventId garantiza idempotencia. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @Column(name = "event_id")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 1000)
    private String summary;

    protected AuditLog() { } // requerido por JPA

    private AuditLog(UUID id, UUID tenantId, String eventType, String entityType,
                     UUID entityId, UUID actorUserId, LocalDateTime occurredAt, String summary) {
        this.id = id;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorUserId = actorUserId;
        this.occurredAt = occurredAt;
        this.summary = summary;
    }

    public static AuditLog from(ProjectCreated event) {
        return new AuditLog(event.eventId(), event.tenantId(), "ProjectCreated", "PROJECT",
                event.projectId(), event.actorUserId(), event.occurredAt(),
                "Proyecto creado: " + event.projectName());
    }

    public static AuditLog from(ProjectStageClosed event) {
        return new AuditLog(event.eventId(), event.tenantId(), "ProjectStageClosed", "PROJECT_STAGE",
                event.stageId(), event.actorUserId(), event.occurredAt(),
                "Etapa cerrada: " + event.stageName() + " (" + event.stageOrder() + ")");
    }

    public static AuditLog from(ProjectAssignmentChanged event) {
        return new AuditLog(event.eventId(), event.tenantId(), event.eventType(),
                "PROJECT_ASSIGNMENT", event.assignmentId(), event.actorUserId(),
                event.occurredAt(), event.summary());
    }

    public static AuditLog from(MembershipRevoked event) {
        return new AuditLog(event.eventId(), event.tenantId(), "MembershipRevoked",
                "MEMBERSHIP", event.membershipId(), event.actorUserId(),
                event.occurredAt(), "Membresía revocada para el usuario " + event.userId());
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public UUID getActorUserId() { return actorUserId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getSummary() { return summary; }
}
