package com.saas.constructora.modules.identity.interfaces.dto;

import com.saas.constructora.modules.identity.domain.Tenant;
import com.saas.constructora.modules.identity.domain.User;
import java.util.UUID;

/**
 * HU-01: respuesta del login/registro con Google.
 * {@code newUser} indica si la cuenta se creó en esta misma petición
 * (es decir, si fue un "registro" y no solo un "inicio de sesión").
 */
public record GoogleAuthResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    boolean newUser,
    TenantResponse tenant,
    LoginResponse.UserResponse user
) {
    public record TenantResponse(UUID id, String name, String slug) {
        static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getName(), t.getSlug());
        }
    }

    public static GoogleAuthResponse from(String accessToken, long expiresInSeconds,
                                          boolean newUser, Tenant tenant, User user) {
        return new GoogleAuthResponse(accessToken, "Bearer", expiresInSeconds, newUser,
                TenantResponse.from(tenant), LoginResponse.UserResponse.from(user));
    }
}
