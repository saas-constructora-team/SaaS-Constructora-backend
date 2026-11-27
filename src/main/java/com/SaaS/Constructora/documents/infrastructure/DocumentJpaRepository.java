package com.SaaS.Constructora.documents.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentJpaRepository extends JpaRepository<DocumentEntity, UUID> {

    Optional<DocumentEntity> findByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    List<DocumentEntity> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId);
}
