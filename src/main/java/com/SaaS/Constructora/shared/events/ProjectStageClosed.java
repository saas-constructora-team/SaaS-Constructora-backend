package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/** Evento publicado cuando un usuario cierra una etapa de un proyecto. */
public record ProjectStageClosed(
        UUID eventId,
        UUID tenantId,
        UUID projectId,
        UUID stageId,
        UUID actorUserId,
        String stageName,
        int stageOrder,
        LocalDateTime occurredAt
) { }
