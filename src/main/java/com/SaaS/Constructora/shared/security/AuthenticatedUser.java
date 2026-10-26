package com.SaaS.Constructora.shared.security;

import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.UUID;

/**
 * Contexto del usuario autenticado, construido exclusivamente a partir de los
 * claims de un JWT ya verificado (firma, expiración e issuer).
 * <p>
 * Los endpoints de negocio reciben este objeto en lugar de leer cabeceras
 * como {@code X-Tenant-Id}: el tenant y el rol nunca los elige el cliente.
 */
public record AuthenticatedUser(UUID userId, UUID tenantId, Role role) {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(role, "role");
    }

    /** Construye el contexto validando los claims del token. */
    public static AuthenticatedUser from(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt");
        return new AuthenticatedUser(
                TenantClaims.requireUserId(jwt),
                TenantClaims.requireTenantId(jwt),
                TenantClaims.requireRole(jwt));
    }

    /** Exige un rol con permisos de administración de la empresa. */
    public void requireAdmin() {
        if (!role.canManageAssignments()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador");
        }
    }
}
