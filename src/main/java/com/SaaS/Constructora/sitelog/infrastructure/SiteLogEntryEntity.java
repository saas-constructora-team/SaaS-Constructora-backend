package com.SaaS.Constructora.sitelog.infrastructure;

import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;
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

/** Entidad de persistencia: referencias por ID, sin asociaciones JPA cruzadas. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "site_log_entries")
public class SiteLogEntryEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "stage_id", nullable = false, updatable = false)
    private UUID stageId;

    @Column(name = "document_id", nullable = false, updatable = false)
    private UUID documentId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SiteLogEntryEntity() { } // requerido por JPA

    public SiteLogEntryEntity(SiteLogEntry entry) {
        this.id = entry.id();
        this.tenantId = entry.tenantId();
        this.projectId = entry.projectId();
        this.stageId = entry.stageId();
        this.documentId = entry.documentId();
        this.createdBy = entry.createdBy();
        this.createdAt = entry.createdAt();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public SiteLogEntry toDomain() {
        return new SiteLogEntry(id, tenantId, projectId, stageId, documentId, createdBy, createdAt);
    }
}
