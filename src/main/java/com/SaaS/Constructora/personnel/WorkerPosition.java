package com.SaaS.Constructora.personnel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cargo (posición) de un trabajador dentro de una empresa constructora.
 * <p>
 * A diferencia de un enum fijo, los cargos son datos propios de cada tenant:
 * se pueden crear, renombrar, desactivar y eliminar. El tenant se referencia
 * por ID y todas las consultas lo filtran explícitamente.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "worker_positions", uniqueConstraints = @UniqueConstraint(
        name = "uk_worker_positions_tenant_name",
        columnNames = {"tenant_id", "name"}))
public class WorkerPosition {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WorkerPosition() { } // requerido por JPA

    private WorkerPosition(UUID tenantId, String name, String description) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.active = true;
    }

    public static WorkerPosition create(UUID tenantId, String name, String description) {
        return new WorkerPosition(tenantId, name, description);
    }

    /** Cambia el nombre del cargo. */
    public void rename(String name) {
        this.name = name;
    }

    /** Cambia la descripción del cargo. */
    public void describe(String description) {
        this.description = description;
    }

    /** Activa o desactiva el cargo sin eliminarlo. */
    public void activate(boolean active) {
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
