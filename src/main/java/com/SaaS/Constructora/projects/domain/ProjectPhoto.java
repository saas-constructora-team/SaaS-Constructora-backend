package com.SaaS.Constructora.projects.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evidencia fotográfica de una etapa. El binario vive en Object Storage y en
 * PostgreSQL solo se guardan los metadatos y la clave del objeto, nunca bytes.
 */
public record ProjectPhoto(
        UUID id,
        UUID tenantId,
        UUID projectId,
        UUID stageId,
        String objectKey,
        String contentType,
        long sizeBytes,
        String sha256,
        String originalName,
        UUID uploadedBy,
        LocalDateTime createdAt
) { }
