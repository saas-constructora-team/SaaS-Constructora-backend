package com.SaaS.Constructora.sitelog.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteLogRecordJpaRepository extends JpaRepository<SiteLogRecordEntity, UUID> {

    Optional<SiteLogRecordEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<SiteLogRecordEntity> findAllByTenantIdAndProjectIdOrderByEventAtDesc(
            UUID tenantId, UUID projectId);

    void deleteByIdAndTenantId(UUID id, UUID tenantId);
}
