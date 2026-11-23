package com.SaaS.Constructora.documents.infrastructure;

import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.Document;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/** Persistencia de documents. El tenant viaja en el contexto autenticado. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetKind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DocumentEntity() { } // requerido por JPA

    public DocumentEntity(Document document) {
        this.id = document.getId();
        this.tenantId = document.getTenantId();
        this.projectId = document.getProjectId();
        this.kind = document.getKind();
        this.title = document.getTitle();
        this.currentVersionId = document.getCurrentVersionId();
        this.createdBy = document.getCreatedBy();
        this.createdAt = document.getCreatedAt();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void pointTo(UUID versionId) {
        this.currentVersionId = versionId;
    }

    public Document toDomain() {
        return Document.rehydrate(id, tenantId, projectId, kind, title,
                currentVersionId, createdBy, createdAt);
    }
}
