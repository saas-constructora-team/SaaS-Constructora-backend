package com.SaaS.Constructora.projects.infrastructure;

import com.SaaS.Constructora.projects.domain.AreaTipo;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Área de trabajo propia de un proyecto. */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "project_areas")
public class ProjectArea {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 150)
    private String name;

    /** Tipo inmutable: determina los campos específicos de las tareas del área. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false, length = 40)
    private AreaTipo tipo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectArea() { } // requerido por JPA

    public ProjectArea(UUID tenantId, UUID projectId, String name) {
        this(tenantId, projectId, name, AreaTipo.PERSONALIZADA);
    }

    public ProjectArea(UUID tenantId, UUID projectId, String name, AreaTipo tipo) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.name = name;
        this.tipo = tipo == null ? AreaTipo.PERSONALIZADA : tipo;
    }

    /** Áreas predeterminadas de cada proyecto; se pueden editar o eliminar. */
    public static List<ProjectArea> initialAreas(UUID tenantId, UUID projectId) {
        return List.of(
                new ProjectArea(tenantId, projectId, "Obra / Ingeniería", AreaTipo.OBRA_INGENIERIA),
                new ProjectArea(tenantId, projectId, "Compras y Almacén", AreaTipo.COMPRAS_ALMACEN),
                new ProjectArea(tenantId, projectId, "Diseño y Arquitectura", AreaTipo.DISENO_ARQUITECTURA),
                new ProjectArea(tenantId, projectId, "Legal / Trámites", AreaTipo.LEGAL_TRAMITES),
                new ProjectArea(tenantId, projectId, "Contabilidad / Finanzas", AreaTipo.CONTABILIDAD_FINANZAS));
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void rename(String name) { this.name = name; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public AreaTipo getTipo() { return tipo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
