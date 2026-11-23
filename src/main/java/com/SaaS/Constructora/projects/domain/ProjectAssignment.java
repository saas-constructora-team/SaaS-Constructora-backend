package com.SaaS.Constructora.projects.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asignación de un usuario del tenant a un proyecto.
 * Las referencias a proyecto y usuario se guardan por ID; no se usan
 * asociaciones JPA para mantener desacoplados los módulos.
 * <p>
 * El borrado es lógico: {@code unassignedAt} conserva quién y cuándo retiró la
 * asignación, y permite reasignar al mismo usuario sin perder el historial.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private ProjectRole role;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "unassigned_by")
    private UUID unassignedBy;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    protected ProjectAssignment() { } // requerido por JPA

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId) {
        this(tenantId, projectId, userId, ProjectRole.MEMBER, null);
    }

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId, ProjectRole role) {
        this(tenantId, projectId, userId, role, null);
    }

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId,
                             ProjectRole role, UUID assignedBy) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role != null ? role : ProjectRole.MEMBER;
        this.assignedBy = assignedBy;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    /** Cambia el rol del usuario dentro del proyecto. */
    public void changeRole(ProjectRole role) {
        this.role = role != null ? role : ProjectRole.MEMBER;
    }

    /** Marca la asignación como retirada sin borrar su historial. */
    public void unassign(UUID unassignedBy, LocalDateTime at) {
        this.unassignedAt = at != null ? at : LocalDateTime.now();
        this.unassignedBy = unassignedBy;
    }

    public boolean isActive() {
        return unassignedAt == null;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public ProjectRole getRole() { return role; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public UUID getAssignedBy() { return assignedBy; }
    public LocalDateTime getUnassignedAt() { return unassignedAt; }
    public UUID getUnassignedBy() { return unassignedBy; }
    public int getVersion() { return version; }
}
