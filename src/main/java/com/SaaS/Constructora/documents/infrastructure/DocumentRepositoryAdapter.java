package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.application.ports.DocumentRepository;
import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA del repositorio de metadatos del módulo documents. */
@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository documents;
    private final DocumentVersionJpaRepository versions;

    public DocumentRepositoryAdapter(DocumentJpaRepository documents,
                                     DocumentVersionJpaRepository versions) {
        this.documents = documents;
        this.versions = versions;
    }

    @Override
    public Document save(Document document) {
        // Con IDs asignados, save() usa merge() y el INSERT puede diferirse hasta
        // el commit. saveAndFlush() garantiza que el documento exista antes de
        // insertar su versión y que un fallo de BD se detecte dentro del caso de uso.
        return documents.saveAndFlush(new DocumentEntity(document)).toDomain();
    }

    @Override
    public DocumentVersion saveVersion(DocumentVersion version) {
        return versions.saveAndFlush(new DocumentVersionEntity(version)).toDomain();
    }

    @Override
    public Optional<Document> findByIdAndTenantIdAndProjectId(UUID id, UUID tenantId, UUID projectId) {
        return documents.findByIdAndTenantIdAndProjectId(id, tenantId, projectId)
                .map(DocumentEntity::toDomain);
    }

    @Override
    public List<Document> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId) {
        return documents.findAllByTenantIdAndProjectId(tenantId, projectId).stream()
                .map(DocumentEntity::toDomain).toList();
    }

    @Override
    public List<DocumentVersion> findVersions(UUID tenantId, UUID documentId) {
        return versions.findAllByTenantIdAndDocumentIdOrderByCreatedAtDesc(tenantId, documentId).stream()
                .map(DocumentVersionEntity::toDomain).toList();
    }

    @Override
    public Optional<DocumentVersion> findVersion(UUID tenantId, UUID documentId, UUID versionId) {
        return versions.findByTenantIdAndDocumentIdAndId(tenantId, documentId, versionId)
                .map(DocumentVersionEntity::toDomain);
    }

    @Override
    public boolean existsVersion(UUID documentId, String versionLabel) {
        return versions.existsByDocumentIdAndVersionLabel(documentId, versionLabel);
    }

}
