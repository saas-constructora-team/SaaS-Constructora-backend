package com.SaaS.Constructora.sitelog.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SiteLogEntryJpaRepository extends JpaRepository<SiteLogEntryEntity, UUID> {

    boolean existsByDocumentIdAndTenantId(UUID documentId, UUID tenantId);

    List<SiteLogEntryEntity> findByTenantIdAndProjectIdAndStageIdOrderByCreatedAtDesc(
            UUID tenantId, UUID projectId, UUID stageId);
}
