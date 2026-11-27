package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.domain.DocumentVersionChange;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/** Historial de objetos S3 generados para cada versión de un documento. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "document_version_changes")
public class DocumentVersionChangeEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false, updatable = false)
    private UUID versionId;

    @Column(name = "object_key", nullable = false, length = 1024, updatable = false)
    private String objectKey;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "pruned_at")
    private LocalDateTime prunedAt;

    protected DocumentVersionChangeEntity() { } // requerido por JPA

    public DocumentVersionChangeEntity(DocumentVersionChange change) {
        this.id = change.id();
        this.tenantId = change.tenantId();
        this.documentId = change.documentId();
        this.versionId = change.versionId();
        this.objectKey = change.objectKey();
        this.changedAt = change.changedAt();
        this.prunedAt = change.prunedAt();
    }

    public void markPruned(LocalDateTime prunedAt) {
        this.prunedAt = prunedAt;
    }

    public DocumentVersionChange toDomain() {
        return new DocumentVersionChange(id, tenantId, documentId, versionId,
                objectKey, changedAt, prunedAt);
    }
}
