package com.SaaS.Constructora.documents.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cada binario almacenado para una versión. Una versión puede tener varios
 * cambios cuando se sobrescribe; solo se conservan en S3 los más recientes.
 */
public record DocumentVersionChange(
        UUID id,
        UUID tenantId,
        UUID documentId,
        UUID versionId,
        String objectKey,
        LocalDateTime changedAt,
        LocalDateTime prunedAt
) { }
