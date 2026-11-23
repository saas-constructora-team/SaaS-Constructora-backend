package com.SaaS.Constructora.documents.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Versión inmutable de un documento. El binario vive en Object Storage y aquí
 * solo se guarda la clave del objeto, nunca los bytes.
 */
public record DocumentVersion(
        UUID id,
        UUID tenantId,
        UUID documentId,
        String versionLabel,
        String objectKey,
        String contentType,
        long sizeBytes,
        String sha256,
        String originalName,
        UUID uploadedBy,
        LocalDateTime createdAt
) { }
