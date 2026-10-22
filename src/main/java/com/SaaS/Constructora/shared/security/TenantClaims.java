package com.SaaS.Constructora.shared.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Extrae el tenant (empresa) autenticado del JWT.
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
}
