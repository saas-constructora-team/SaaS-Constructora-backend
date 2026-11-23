package com.SaaS.Constructora.documents.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Documento técnico de un proyecto. Apunta a la versión vigente; las versiones
 * anteriores se conservan como filas inmutables en {@link DocumentVersion}.
 */
public final class Document {

    private final UUID id;
    private final UUID tenantId;
    private final UUID projectId;
    private final AssetKind kind;
    private final String title;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private UUID currentVersionId;

    private Document(UUID id, UUID tenantId, UUID projectId, AssetKind kind, String title,
                     UUID currentVersionId, UUID createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.kind = kind;
        this.title = title;
        this.currentVersionId = currentVersionId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static Document create(UUID id, UUID tenantId, UUID projectId, AssetKind kind,
                                  String title, UUID currentVersionId, UUID createdBy) {
        return new Document(id, tenantId, projectId, kind, title.trim(),
                currentVersionId, createdBy, LocalDateTime.now());
    }

    public static Document rehydrate(UUID id, UUID tenantId, UUID projectId, AssetKind kind,
                                     String title, UUID currentVersionId, UUID createdBy,
                                     LocalDateTime createdAt) {
        return new Document(id, tenantId, projectId, kind, title, currentVersionId,
                createdBy, createdAt);
    }

    /** Actualiza el puntero a la versión vigente. Solo lo invoca el caso de uso de carga. */
    public void pointTo(UUID versionId) {
        this.currentVersionId = versionId;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public AssetKind getKind() { return kind; }
    public String getTitle() { return title; }
    public UUID getCurrentVersionId() { return currentVersionId; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
