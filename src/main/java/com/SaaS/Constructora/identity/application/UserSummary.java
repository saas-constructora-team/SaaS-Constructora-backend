package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.User;

import java.util.UUID;

/**
 * Vista pública y segura de un usuario del tenant.
 * No expone el hash de contraseña y evita que otros módulos dependan de
 * {@link User} o de la infraestructura de identity.
 */
public record UserSummary(
        UUID id,
        String email,
        String fullName,
        String username,
        Role role,
        boolean active,
        boolean canCreatePersonnel,
        boolean canViewAssignmentCandidates,
        boolean canCreateTasks,
        boolean canManageSiteLog
) {
    public UserSummary(UUID id, String email, String fullName, Role role, boolean active) {
        this(id, email, fullName, null, role, active, role.canCreatePersonnel(),
                role.canViewAssignmentCandidates(), role.canCreateTasks(),
                role.canManageSiteLog());
    }

    public UserSummary(UUID id, String email, String fullName, Role role, boolean active,
                       boolean canCreatePersonnel, boolean canViewAssignmentCandidates,
                       boolean canCreateTasks) {
        this(id, email, fullName, null, role, active, canCreatePersonnel,
                canViewAssignmentCandidates, canCreateTasks, role.canManageSiteLog());
    }

    public static UserSummary from(User user, Membership membership) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                user.getUsername(), membership.getRole(), user.isActive(),
                membership.canCreatePersonnel(), membership.canViewAssignmentCandidates(),
                membership.canCreateTasks(), membership.canManageSiteLog());
    }

    public boolean canManageAssignments() {
        return role.canManageAssignments();
    }
}
