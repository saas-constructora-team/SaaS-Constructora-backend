package com.SaaS.Constructora.sitelog.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entrada de la bitácora de una obra. Se asocia solo al proyecto: la etapa no
 * forma parte del contrato actual. El autor se deriva del usuario autenticado
 * y nunca se toma del cuerpo de la petición.
 */
public record SiteLogRecord(
        UUID id,
        UUID tenantId,
        UUID projectId,
        SiteLogType type,
        SiteLogStatus status,
        String what,
        String how,
        LocalDateTime eventAt,
        String location,
        String why,
        UUID authorId,
        UUID assigneeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public SiteLogRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(projectId, "projectId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(eventAt, "when");
        Objects.requireNonNull(authorId, "authorId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        requireText(what, "what");
        requireText(how, "how");
        requireText(location, "where");
    }

    /** Copia con la fecha de modificación actualizada, sin tocar `createdAt`. */
    public SiteLogRecord touched(LocalDateTime now) {
        return new SiteLogRecord(id, tenantId, projectId, type, status, what, how,
                eventAt, location, why, authorId, assigneeId, createdAt, now);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El campo " + field + " es obligatorio");
        }
    }
}
