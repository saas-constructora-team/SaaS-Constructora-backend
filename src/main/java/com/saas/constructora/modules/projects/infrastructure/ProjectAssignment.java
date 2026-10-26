package com.saas.constructora.modules.projects.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asignación de un usuario del tenant a un proyecto.
 * Las referencias a proyecto y usuario se guardan por ID; no se usan
 * asociaciones JPA para mantener desacoplados los módulos.
 */
@Entity
@Table(name = "project_assignments")
public class ProjectAssignment {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    protected ProjectAssignment() { } // requerido por JPA

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
