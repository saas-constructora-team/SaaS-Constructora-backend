package com.SaaS.Constructora.sitelog.application.ports;

import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;

import java.util.List;
import java.util.UUID;

/** Persistencia de asociaciones document-etapa, siempre filtrada por tenant. */
public interface SiteLogRepository {

    SiteLogEntry save(SiteLogEntry entry);

    boolean existsByDocumentIdAndTenantId(UUID documentId, UUID tenantId);

    List<SiteLogEntry> findByTenantIdAndProjectIdAndStageId(
            UUID tenantId, UUID projectId, UUID stageId);
}
