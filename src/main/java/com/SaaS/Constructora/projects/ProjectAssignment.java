package com.SaaS.Constructora.projects;

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
 * El {@link ProjectRole} es la delegación de permisos dentro del proyecto:
 * un ADMIN del tenant puede nombrar LEAD a un usuario para que gestione
 * los miembros de ese proyecto sin convertirlo en ADMIN de la empresa.
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

    protected ProjectAssignment() { } // requerido por JPA

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId) {
        this(tenantId, projectId, userId, ProjectRole.MEMBER);
    }

    public ProjectAssignment(UUID tenantId, UUID projectId, UUID userId, ProjectRole role) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role != null ? role : ProjectRole.MEMBER;
    }

    @PrePersist
    void onCreate() {
        if (assignedAt == null) assignedAt = LocalDateTime.now();
    }

    /** Cambia el rol del usuario dentro del proyecto. */
    public void changeRole(ProjectRole role) {
        this.role = role != null ? role : ProjectRole.MEMBER;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public ProjectRole getRole() { return role; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
