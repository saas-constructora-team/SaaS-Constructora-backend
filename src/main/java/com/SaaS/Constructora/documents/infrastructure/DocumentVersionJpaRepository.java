package com.SaaS.Constructora.documents.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findAllByTenantIdAndDocumentIdOrderByCreatedAtDesc(
            UUID tenantId, UUID documentId);

    Optional<DocumentVersionEntity> findByTenantIdAndDocumentIdAndId(
            UUID tenantId, UUID documentId, UUID id);

    boolean existsByDocumentIdAndVersionLabel(UUID documentId, String versionLabel);
}
