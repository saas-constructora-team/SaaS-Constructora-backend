package com.saas.constructora.modules.identity.application;

import com.saas.constructora.modules.identity.domain.Role;
import com.saas.constructora.modules.identity.domain.User;

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
        boolean active
) {
    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.isActive());
    }
}
