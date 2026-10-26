package com.SaaS.Constructora.projects;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectStageResponse(
        UUID id,
        UUID projectId,
        String name,
        int order,
        LocalDateTime createdAt
) {
    static ProjectStageResponse from(ProjectStage stage) {
        return new ProjectStageResponse(
                stage.getId(),
                stage.getProjectId(),
                stage.getName(),
                stage.getStageOrder(),
                stage.getCreatedAt());
    }
}
