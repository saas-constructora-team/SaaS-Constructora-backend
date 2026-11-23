package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectStageResponse(
        UUID id,
        UUID projectId,
        String name,
        int order,
        LocalDateTime createdAt,
        LocalDateTime closedAt
) {
    public static ProjectStageResponse from(ProjectStage stage) {
        return new ProjectStageResponse(
                stage.getId(),
                stage.getProjectId(),
                stage.getName(),
                stage.getStageOrder(),
                stage.getCreatedAt(),
                stage.getClosedAt());
    }
}
