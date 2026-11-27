package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.application.UserSummary;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String role,
        boolean active,
        boolean canCreatePersonnel,
        boolean canViewAssignmentCandidates,
        boolean canCreateTasks,
        boolean canManageSiteLog
) {
    public static UserResponse from(UserSummary user) {
        return new UserResponse(user.id(), user.email(), user.fullName(),
                user.role().name(), user.active(), user.canCreatePersonnel(),
                user.canViewAssignmentCandidates(), user.canCreateTasks(),
                user.canManageSiteLog());
    }
}
