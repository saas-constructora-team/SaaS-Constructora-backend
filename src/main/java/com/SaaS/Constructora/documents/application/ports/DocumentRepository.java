package com.SaaS.Constructora.documents.application.ports;

import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de metadatos del módulo documents, siempre filtrada por tenant. */
public interface DocumentRepository {

    Document save(Document document);

    DocumentVersion saveVersion(DocumentVersion version);

    Optional<Document> findByIdAndTenantIdAndProjectId(UUID id, UUID tenantId, UUID projectId);

    List<Document> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId);

    List<DocumentVersion> findVersions(UUID tenantId, UUID documentId);

    Optional<DocumentVersion> findVersion(UUID tenantId, UUID documentId, UUID versionId);

    boolean existsVersion(UUID documentId, String versionLabel);

}
