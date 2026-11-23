package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectAreaResponse(
        UUID id,
        UUID projectId,
        String name,
        AreaTipo tipo,
        LocalDateTime createdAt
) {
    public static ProjectAreaResponse from(ProjectArea area) {
        return new ProjectAreaResponse(
                area.getId(),
                area.getProjectId(),
                area.getName(),
                area.getTipo(),
                area.getCreatedAt());
    }
}
