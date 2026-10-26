package com.saas.constructora.shared.security;

import com.saas.constructora.modules.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Extrae del JWT los datos de contexto del usuario autenticado.
 * Un token válido sin un {@code tenant_id} utilizable no puede operar sobre
 * datos de negocio: se responde 403 para no romper el aislamiento multi-tenant.
 */
public final class TenantClaims {

    private TenantClaims() { }

    public static UUID requireTenantId(Jwt jwt) {
        String raw = jwt.getClaimAsString(JwtService.TENANT_CLAIM);
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El token no contiene la empresa (tenant) asociada");
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El identificador de empresa del token no es válido");
        }
    }

    /** Identificador del usuario autenticado (claim {@code sub}). */
    public static UUID requireUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El token no contiene el usuario autenticado");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El identificador de usuario del token no es válido");
        }
    }

    /** Rol del usuario autenticado (claim {@code role}). */
    public static Role requireRole(Jwt jwt) {
        String raw = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El token no contiene el rol del usuario");
        }
        try {
            return Role.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "El rol del usuario no es válido");
        }
    }

    /** Exige un rol con permisos de administración de la empresa. */
    public static void requireAdmin(Jwt jwt) {
        if (!requireRole(jwt).canManageAssignments()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador");
        }
    }
}
