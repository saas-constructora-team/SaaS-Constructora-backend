package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando un usuario crea un proyecto.
 * <p>
 * El {@code eventId} se usa como identidad del registro de auditoría para que
 * un reintento del registro de eventos no duplique la entrada.
 */
public record ProjectCreated(
        UUID eventId,
        UUID tenantId,
        UUID projectId,
        UUID actorUserId,
        String projectName,
        LocalDateTime occurredAt
) { }
