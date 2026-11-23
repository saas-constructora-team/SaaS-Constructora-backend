package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado cuando se almacena una nueva versión de un documento.
 * La auditoría (Fase 5) podrá consumirlo sin que documents dependa de ella.
 */
public record DocumentVersionUploaded(
        UUID eventId,
        UUID tenantId,
        UUID projectId,
        UUID documentId,
        UUID versionId,
        UUID actorUserId,
        String versionLabel,
        String originalName,
        long sizeBytes,
        LocalDateTime occurredAt
) { }
