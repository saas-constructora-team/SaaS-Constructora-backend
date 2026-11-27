package com.SaaS.Constructora.sitelog.infrastructure;

import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogStatus;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/** Entidad JPA de la bitácora: referencias por ID, sin asociaciones cruzadas. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "site_log_records")
public class SiteLogRecordEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteLogType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteLogStatus status;

    @Column(nullable = false, length = 500)
    private String what;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String how;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Column(nullable = false, length = 300)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String why;

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SiteLogRecordEntity() { } // requerido por JPA

    public SiteLogRecordEntity(SiteLogRecord record) {
        this.id = record.id();
        this.tenantId = record.tenantId();
        this.projectId = record.projectId();
        this.type = record.type();
        this.status = record.status();
        this.what = record.what();
        this.how = record.how();
        this.eventAt = record.eventAt();
        this.location = record.location();
        this.why = record.why();
        this.authorId = record.authorId();
        this.assigneeId = record.assigneeId();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public SiteLogRecord toDomain() {
        return new SiteLogRecord(id, tenantId, projectId, type, status, what, how,
                eventAt, location, why, authorId, assigneeId, createdAt, updatedAt);
    }
}
