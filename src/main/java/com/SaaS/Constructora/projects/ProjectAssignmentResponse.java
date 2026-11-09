package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.UserSummary;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista de una asignación de proyecto. {@code userRole} es el rol del usuario
 * en la empresa ({@code ADMIN}/{@code MEMBER}) y {@code projectRole} es su
 * rol dentro del proyecto ({@code LEAD}/{@code MEMBER}).
 */
public record ProjectAssignmentResponse(
    UUID id,
    UUID projectId,
    UUID userId,
    String userFullName,
    String userEmail,
    String userRole,
    ProjectRole projectRole,
    LocalDateTime assignedAt
) {
    static ProjectAssignmentResponse from(ProjectAssignment assignment, UserSummary user) {
        return new ProjectAssignmentResponse(
                assignment.getId(),
                assignment.getProjectId(),
                assignment.getUserId(),
                user != null ? user.fullName() : null,
                user != null ? user.email() : null,
                user != null ? user.role().name() : null,
                assignment.getRole(),
                assignment.getAssignedAt());
    }
}
