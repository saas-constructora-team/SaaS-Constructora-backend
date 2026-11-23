package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento de integración publicado cuando cambia la membresía de un proyecto. */
public record ProjectAssignmentChanged(
        UUID eventId,
        UUID tenantId,
        UUID projectId,
        UUID assignmentId,
        UUID userId,
        UUID actorUserId,
        String eventType,
        String summary,
        LocalDateTime occurredAt) {
}
