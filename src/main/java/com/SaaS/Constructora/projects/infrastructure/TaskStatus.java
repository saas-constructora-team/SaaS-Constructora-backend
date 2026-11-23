package com.SaaS.Constructora.projects.infrastructure;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado configurable de las tareas de un proyecto. Cada proyecto tiene su
 * propio catálogo y exactamente un estado por defecto.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "task_statuses")
public class TaskStatus {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(name = "status_order", nullable = false, updatable = false)
    private int statusOrder;

    @Column(name = "is_default", nullable = false)
    private boolean defaultStatus;

    /** Estado del sistema: se puede editar, pero no eliminar. */
    @Column(name = "is_system", nullable = false)
    private boolean systemStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TaskStatus() { } // requerido por JPA

    public TaskStatus(UUID tenantId, UUID projectId, String name, String color,
                      int statusOrder, boolean defaultStatus) {
        this(tenantId, projectId, name, color, statusOrder, defaultStatus, false);
    }

    public TaskStatus(UUID tenantId, UUID projectId, String name, String color,
                      int statusOrder, boolean defaultStatus, boolean systemStatus) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.name = name;
        this.color = color;
        this.statusOrder = statusOrder;
        this.defaultStatus = defaultStatus;
        this.systemStatus = systemStatus;
    }

    public static List<TaskStatus> initialStatuses(UUID tenantId, UUID projectId) {
        List<TaskStatus> statuses = new ArrayList<>(3);
        statuses.add(new TaskStatus(tenantId, projectId, "Pendiente", "#EF4444", 1, true));
        statuses.add(new TaskStatus(tenantId, projectId, "En curso", "#F59E0B", 2, false));
        statuses.add(new TaskStatus(tenantId, projectId, "Terminado", "#22C55E", 3, false, true));
        return statuses;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** Actualiza nombre, color y, si se indica, el estado por defecto. */
    public void update(String name, String color, Boolean makeDefault) {
        this.name = name;
        this.color = color;
        if (makeDefault != null) {
            this.defaultStatus = makeDefault;
        }
    }

    public void markAsDefault() { this.defaultStatus = true; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public int getStatusOrder() { return statusOrder; }
    public boolean isDefaultStatus() { return defaultStatus; }
    public boolean isSystemStatus() { return systemStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
