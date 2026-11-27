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
        Role role,
        boolean active,
        boolean canCreatePersonnel,
        boolean canViewAssignmentCandidates,
        boolean canCreateTasks,
        boolean canManageSiteLog
) {
    /** Constructor conservado para los consumidores que no necesitan los permisos. */
    public UserSummary(UUID id, String email, String fullName, Role role, boolean active) {
        this(id, email, fullName, role, active, role.canCreatePersonnel(),
                role.canViewAssignmentCandidates(), role.canCreateTasks(),
                role.canManageSiteLog());
    }

    /** Constructor conservado para consumidores que no usan el permiso de bitácora. */
    public UserSummary(UUID id, String email, String fullName, Role role, boolean active,
                       boolean canCreatePersonnel, boolean canViewAssignmentCandidates,
                       boolean canCreateTasks) {
        this(id, email, fullName, role, active, canCreatePersonnel,
                canViewAssignmentCandidates, canCreateTasks, role.canManageSiteLog());
    }

    public static UserSummary from(User user, Membership membership) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                membership.getRole(), user.isActive(), membership.canCreatePersonnel(),
                membership.canViewAssignmentCandidates(), membership.canCreateTasks(),
                membership.canManageSiteLog());
    }

    /** Autoridad vigente en BD para gestionar asignaciones (no viene del JWT). */
    public boolean canManageAssignments() {
        return role.canManageAssignments();
    }
}
