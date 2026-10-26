package com.saas.constructora.modules.projects.interfaces.dto;

import com.saas.constructora.modules.identity.application.UserSummary;
import com.saas.constructora.modules.projects.infrastructure.ProjectAssignment;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectAssignmentResponse(
    UUID id,
    UUID projectId,
    UUID userId,
    String userFullName,
    String userEmail,
    String userRole,
    LocalDateTime assignedAt
) {
    public static ProjectAssignmentResponse from(ProjectAssignment assignment, UserSummary user) {
        return new ProjectAssignmentResponse(
                assignment.getId(),
                assignment.getProjectId(),
                assignment.getUserId(),
                user != null ? user.fullName() : null,
                user != null ? user.email() : null,
                user != null ? user.role().name() : null,
                assignment.getAssignedAt());
    }
}
