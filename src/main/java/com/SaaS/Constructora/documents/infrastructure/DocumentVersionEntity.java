package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.domain.DocumentVersion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Versión de documento. En el modo append-only es inmutable; la sobrescritura
 * actualiza su contenido sin cambiar su identidad ni su fecha de creación.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "document_versions")
public class DocumentVersionEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "version_label", nullable = false, length = 20, updatable = false)
    private String versionLabel;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected DocumentVersionEntity() { } // requerido por JPA

    public DocumentVersionEntity(DocumentVersion version) {
        this.id = version.id();
        this.tenantId = version.tenantId();
        this.documentId = version.documentId();
        this.versionLabel = version.versionLabel();
        this.objectKey = version.objectKey();
        this.contentType = version.contentType();
        this.sizeBytes = version.sizeBytes();
        this.sha256 = version.sha256();
        this.originalName = version.originalName();
        this.uploadedBy = version.uploadedBy();
        this.createdAt = version.createdAt();
        this.changedAt = version.changedAt() == null ? version.createdAt() : version.changedAt();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (changedAt == null) changedAt = createdAt;
    }

    /** Aplica la sobrescritura sobre la misma fila, conservando id y fecha de creación. */
    public void applyOverwrite(DocumentVersion version) {
        this.objectKey = version.objectKey();
        this.contentType = version.contentType();
        this.sizeBytes = version.sizeBytes();
        this.sha256 = version.sha256();
        this.originalName = version.originalName();
        this.uploadedBy = version.uploadedBy();
        this.changedAt = version.changedAt() == null ? LocalDateTime.now() : version.changedAt();
    }

    public DocumentVersion toDomain() {
        return new DocumentVersion(id, tenantId, documentId, versionLabel, objectKey,
                contentType, sizeBytes, sha256, originalName, uploadedBy, createdAt,
                changedAt == null ? createdAt : changedAt, true);
    }
}
