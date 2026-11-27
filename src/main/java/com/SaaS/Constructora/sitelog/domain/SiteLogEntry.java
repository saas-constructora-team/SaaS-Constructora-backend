package com.SaaS.Constructora.sitelog.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asociación 1:1 entre un documento y una etapa de un proyecto. Solo guarda
 * referencias por ID y metadatos; el binario vive en Neon Object Storage.
 */
public record SiteLogEntry(
        UUID id,
        UUID tenantId,
        UUID projectId,
        UUID stageId,
        UUID documentId,
        UUID createdBy,
        LocalDateTime createdAt
) { }
