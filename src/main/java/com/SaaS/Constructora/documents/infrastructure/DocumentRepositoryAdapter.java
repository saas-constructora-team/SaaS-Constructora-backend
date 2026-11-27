package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.application.ports.DocumentRepository;
import com.SaaS.Constructora.documents.domain.Document;
import com.SaaS.Constructora.documents.domain.DocumentVersion;
import com.SaaS.Constructora.documents.domain.DocumentVersionChange;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA del repositorio de metadatos del módulo documents. */
@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository documents;
    private final DocumentVersionJpaRepository versions;
    private final DocumentVersionChangeJpaRepository changes;

    public DocumentRepositoryAdapter(DocumentJpaRepository documents,
                                     DocumentVersionJpaRepository versions,
                                     DocumentVersionChangeJpaRepository changes) {
        this.documents = documents;
        this.versions = versions;
        this.changes = changes;
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
    public DocumentVersion updateVersion(DocumentVersion version) {
        DocumentVersionEntity entity = versions.findByIdAndTenantId(version.id(), version.tenantId())
                .orElseThrow(() -> new IllegalStateException("Versión no encontrada para actualizar"));
        entity.applyOverwrite(version);
        return versions.saveAndFlush(entity).toDomain();
    }

    @Override
    public Optional<Document> findByIdAndTenantIdAndProjectId(UUID id, UUID tenantId, UUID projectId) {
        return documents.findByIdAndTenantIdAndProjectId(id, tenantId, projectId)
                .map(DocumentEntity::toDomain);
    }

    @Override
    public boolean existsByIdAndTenantId(UUID id, UUID tenantId) {
        return documents.existsByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<Document> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId) {
        return documents.findAllByTenantIdAndProjectId(tenantId, projectId).stream()
                .map(DocumentEntity::toDomain).toList();
    }

    @Override
    public List<DocumentVersion> findVersions(UUID tenantId, UUID documentId) {
        return versions.findAllByTenantIdAndDocumentIdOrderByCreatedAtDesc(tenantId, documentId).stream()
                .map(entity -> withAvailability(tenantId, entity.toDomain()))
                .toList();
    }

    @Override
    public Optional<DocumentVersion> findVersion(UUID tenantId, UUID documentId, UUID versionId) {
        return versions.findByTenantIdAndDocumentIdAndId(tenantId, documentId, versionId)
                .map(entity -> withAvailability(tenantId, entity.toDomain()));
    }

    @Override
    public Optional<DocumentVersion> findCurrentVersion(UUID tenantId, UUID documentId, UUID versionId) {
        if (versionId == null) {
            return Optional.empty();
        }
        return findVersion(tenantId, documentId, versionId);
    }

    @Override
    public boolean existsVersion(UUID documentId, String versionLabel) {
        return versions.existsByDocumentIdAndVersionLabel(documentId, versionLabel);
    }

    @Override
    public DocumentVersionChange recordChange(DocumentVersionChange change) {
        return changes.saveAndFlush(new DocumentVersionChangeEntity(change)).toDomain();
    }

    @Override
    public List<DocumentVersionChange> listChanges(UUID tenantId, UUID documentId) {
        return changes.findAllByTenantIdAndDocumentIdOrderByChangedAtDesc(tenantId, documentId).stream()
                .map(DocumentVersionChangeEntity::toDomain)
                .toList();
    }

    /** Se ejecuta después del commit, por eso necesita su propia transacción. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markChangePruned(UUID tenantId, UUID changeId, LocalDateTime prunedAt) {
        changes.findByIdAndTenantId(changeId, tenantId).ifPresent(change -> {
            change.markPruned(prunedAt);
            changes.saveAndFlush(change);
        });
    }

    @Override
    public boolean isObjectPruned(UUID tenantId, String objectKey) {
        return changes.existsByTenantIdAndObjectKeyAndPrunedAtIsNotNull(tenantId, objectKey);
    }

    private DocumentVersion withAvailability(UUID tenantId, DocumentVersion version) {
        return isObjectPruned(tenantId, version.objectKey()) ? version.withoutBinary() : version;
    }

}
