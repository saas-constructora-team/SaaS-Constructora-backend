package com.SaaS.Constructora.projects.infrastructure;

import com.SaaS.Constructora.projects.domain.ProjectPhoto;
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

/** Persistencia de metadatos de evidencia fotográfica; el binario no está aquí. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "project_photos")
public class ProjectPhotoEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "stage_id", nullable = false, updatable = false)
    private UUID stageId;

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

    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectPhotoEntity() { } // requerido por JPA

    public ProjectPhotoEntity(ProjectPhoto photo) {
        this.id = photo.id();
        this.tenantId = photo.tenantId();
        this.projectId = photo.projectId();
        this.stageId = photo.stageId();
        this.objectKey = photo.objectKey();
        this.contentType = photo.contentType();
        this.sizeBytes = photo.sizeBytes();
        this.sha256 = photo.sha256();
        this.originalName = photo.originalName();
        this.uploadedBy = photo.uploadedBy();
        this.createdAt = photo.createdAt();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public ProjectPhoto toDomain() {
        return new ProjectPhoto(id, tenantId, projectId, stageId, objectKey, contentType,
                sizeBytes, sha256, originalName, uploadedBy, createdAt);
    }
}
