package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.infrastructure.Project;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Avance compartido de un proyecto. {@code version} permite detectar cambios
 * concurrentes: si otra persona seleccionó una etapa antes, el PUT devuelve 409.
 */
public record ProjectProgressResponse(
        UUID projectId,
        UUID currentStageId,
        int version,
        LocalDateTime updatedAt,
        UUID updatedBy
) {
    public static ProjectProgressResponse from(Project project) {
        return new ProjectProgressResponse(
                project.getId(), project.getCurrentStageId(), project.getVersion(),
                project.getStageUpdatedAt(), project.getStageUpdatedBy());
    }
}
