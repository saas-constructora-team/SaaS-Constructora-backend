package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificación ligera de que algo cambió en un proyecto. Solo transporta
 * metadatos: quienes reaccionan vuelven a consultar los datos por los
 * endpoints normales, que ya aplican el aislamiento por tenant y los permisos.
 */
public record ProjectChanged(
        UUID eventId,
        UUID tenantId,
        UUID projectId,
        String section,
        UUID entityId,
        UUID actorUserId,
        LocalDateTime occurredAt
) {
    public static final String PROJECT = "PROJECT";
    public static final String STAGE = "STAGE";
    public static final String PROGRESS = "PROGRESS";
    public static final String ASSIGNMENT = "ASSIGNMENT";
    public static final String SITE_LOG = "SITE_LOG";
    public static final String DOCUMENT = "DOCUMENT";

    public static ProjectChanged of(UUID tenantId, UUID projectId, String section,
                                    UUID entityId, UUID actorUserId) {
        return new ProjectChanged(UUID.randomUUID(), tenantId, projectId, section,
                entityId, actorUserId, LocalDateTime.now());
    }
}
