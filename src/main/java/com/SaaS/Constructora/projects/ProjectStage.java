package com.SaaS.Constructora.projects;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Etapa de un proyecto. El orden se asigna al crear la etapa y no cambia al
 * editarla. Las etapas iniciales se crean automáticamente con cada proyecto.
 */
@Entity
@Table(name = "project_stages")
public class ProjectStage {

    public static final List<String> INITIAL_STAGE_NAMES = List.of(
            "Estudios Preliminares y Diagnóstico",
            "Anteproyecto",
            "Proyecto Ejecutivo",
            "Licencias, Permisos y Presupuesto Final",
            "Construcción",
            "Cierre, Liquidación y Entrega");

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "stage_order", nullable = false, updatable = false)
    private int stageOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProjectStage() { } // requerido por JPA

    public ProjectStage(UUID tenantId, UUID projectId, String name, int stageOrder) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.name = name;
        this.stageOrder = stageOrder;
    }

    public static List<ProjectStage> initialStages(UUID tenantId, UUID projectId) {
        List<ProjectStage> stages = new ArrayList<>(INITIAL_STAGE_NAMES.size());
        for (int i = 0; i < INITIAL_STAGE_NAMES.size(); i++) {
            stages.add(new ProjectStage(tenantId, projectId, INITIAL_STAGE_NAMES.get(i), i + 1));
        }
        return stages;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProjectId() { return projectId; }
    public String getName() { return name; }
    public int getStageOrder() { return stageOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
