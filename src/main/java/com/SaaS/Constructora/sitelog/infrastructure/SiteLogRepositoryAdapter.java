package com.SaaS.Constructora.sitelog.infrastructure;

import com.SaaS.Constructora.sitelog.application.ports.SiteLogRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Adaptador JPA del módulo site-log. */
@Repository
public class SiteLogRepositoryAdapter implements SiteLogRepository {

    private final SiteLogEntryJpaRepository repository;

    public SiteLogRepositoryAdapter(SiteLogEntryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SiteLogEntry save(SiteLogEntry entry) {
        return repository.saveAndFlush(new SiteLogEntryEntity(entry)).toDomain();
    }

    @Override
    public boolean existsByDocumentIdAndTenantId(UUID documentId, UUID tenantId) {
        return repository.existsByDocumentIdAndTenantId(documentId, tenantId);
    }

    @Override
    public List<SiteLogEntry> findByTenantIdAndProjectIdAndStageId(
            UUID tenantId, UUID projectId, UUID stageId) {
        return repository.findByTenantIdAndProjectIdAndStageIdOrderByCreatedAtDesc(
                        tenantId, projectId, stageId)
                .stream()
                .map(SiteLogEntryEntity::toDomain)
                .toList();
    }
}
