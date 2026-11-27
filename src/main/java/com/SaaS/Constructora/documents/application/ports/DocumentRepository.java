package com.SaaS.Constructora.documents.application.ports;

import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import com.SaaS.Constructora.documents.domain.DocumentVersionChange;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de metadatos del módulo documents, siempre filtrada por tenant. */
public interface DocumentRepository {

    Document save(Document document);

    DocumentVersion saveVersion(DocumentVersion version);

    /** Reemplaza el contenido de una versión existente sin crear otra fila. */
    DocumentVersion updateVersion(DocumentVersion version);

    Optional<Document> findByIdAndTenantIdAndProjectId(UUID id, UUID tenantId, UUID projectId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    List<Document> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId);

    List<DocumentVersion> findVersions(UUID tenantId, UUID documentId);

    Optional<DocumentVersion> findVersion(UUID tenantId, UUID documentId, UUID versionId);

    Optional<DocumentVersion> findCurrentVersion(UUID tenantId, UUID documentId, UUID versionId);

    boolean existsVersion(UUID documentId, String versionLabel);

    DocumentVersionChange recordChange(DocumentVersionChange change);

    List<DocumentVersionChange> listChanges(UUID tenantId, UUID documentId);

    void markChangePruned(UUID tenantId, UUID changeId, LocalDateTime prunedAt);

    boolean isObjectPruned(UUID tenantId, String objectKey);

}
