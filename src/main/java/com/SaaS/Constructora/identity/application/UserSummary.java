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
        boolean canViewAssignmentCandidates
) {
    /** Constructor conservado para los consumidores que no necesitan el permiso. */
    public UserSummary(UUID id, String email, String fullName, Role role, boolean active) {
        this(id, email, fullName, role, active, role.canCreatePersonnel(), role.canViewAssignmentCandidates());
    }

    public static UserSummary from(User user, Membership membership) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                membership.getRole(), user.isActive(), membership.canCreatePersonnel(),
                membership.canViewAssignmentCandidates());
    }
}
