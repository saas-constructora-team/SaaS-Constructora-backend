package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskStatusResponse(
        UUID id,
        UUID projectId,
        String name,
        String color,
        int order,
        boolean isDefault,
        boolean isSystem,
        LocalDateTime createdAt
) {
    public static TaskStatusResponse from(TaskStatus status) {
        return new TaskStatusResponse(
                status.getId(),
                status.getProjectId(),
                status.getName(),
                status.getColor(),
                status.getStatusOrder(),
                status.isDefaultStatus(),
                status.isSystemStatus(),
                status.getCreatedAt());
    }
}
