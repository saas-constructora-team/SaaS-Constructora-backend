package com.saas.constructora.modules.projects.interfaces.dto;

import com.saas.constructora.modules.projects.infrastructure.Project;
import com.saas.constructora.modules.projects.domain.ProjectStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String name,
    String clientName,
    String location,
    ProjectStatus status,
    LocalDateTime createdAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getClientName(),
                p.getLocation(), p.getStatus(), p.getCreatedAt());
    }
}
