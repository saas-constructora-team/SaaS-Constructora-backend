package com.SaaS.Constructora.documents.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionChangeJpaRepository extends JpaRepository<DocumentVersionChangeEntity, UUID> {

    List<DocumentVersionChangeEntity> findAllByTenantIdAndDocumentIdOrderByChangedAtDesc(
            UUID tenantId, UUID documentId);

    Optional<DocumentVersionChangeEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndObjectKeyAndPrunedAtIsNotNull(UUID tenantId, String objectKey);
}
