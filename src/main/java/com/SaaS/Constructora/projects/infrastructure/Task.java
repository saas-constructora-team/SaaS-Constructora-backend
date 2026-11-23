package com.SaaS.Constructora.projects.infrastructure;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tarea vinculada a un proyecto. El tenant se referencia por ID y toda consulta
 * debe filtrarlo explícitamente. Las referencias a estado, área y usuarios se
 * guardan por ID, sin asociaciones JPA que acoplen otras entidades.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "fecha_fin_prevista")
    private LocalDate fechaFinPrevista;

    @Embedded
    private TaskAreaDetails detallesArea;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "area_id")
    private UUID areaId;

    @Column(name = "assignee_assignment_id")
    private UUID assigneeAssignmentId;

    @Column(name = "validator_assignment_id")
    private UUID validatorAssignmentId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Task() { } // requerido por JPA

    public Task(UUID tenantId, UUID projectId, UUID createdBy, String title, String description,
                UUID statusId, UUID areaId, UUID assigneeAssignmentId,
                UUID validatorAssignmentId) {
        this(tenantId, projectId, createdBy, title, description, null, new TaskAreaDetails(),
                statusId, areaId, assigneeAssignmentId, validatorAssignmentId);
    }

    public Task(UUID tenantId, UUID projectId, UUID createdBy, String title, String description,
                LocalDate fechaFinPrevista, TaskAreaDetails detallesArea,
                UUID statusId, UUID areaId, UUID assigneeAssignmentId,
                UUID validatorAssignmentId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.title = title;
        this.description = description;
        this.fechaFinPrevista = fechaFinPrevista;
        this.detallesArea = detallesArea == null ? new TaskAreaDetails() : detallesArea;
        this.statusId = statusId;
        this.areaId = areaId;
        this.assigneeAssignmentId = assigneeAssignmentId;
        this.validatorAssignmentId = validatorAssignmentId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getFechaFinPrevista() { return fechaFinPrevista; }
    public TaskAreaDetails getDetallesArea() { return detallesArea; }
    public UUID getStatusId() { return statusId; }
    public UUID getAreaId() { return areaId; }
    public UUID getAssigneeAssignmentId() { return assigneeAssignmentId; }
    public UUID getValidatorAssignmentId() { return validatorAssignmentId; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
