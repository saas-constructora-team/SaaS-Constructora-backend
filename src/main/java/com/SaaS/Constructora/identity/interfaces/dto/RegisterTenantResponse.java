package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import java.util.UUID;

public record RegisterTenantResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    TenantResponse tenant,
    LoginResponse.UserResponse user
) {
    public record TenantResponse(UUID id, String name, String slug) {
        static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getName(), t.getSlug());
        }
    }

    public static RegisterTenantResponse from(String accessToken, long expiresInSeconds,
                                              Tenant tenant, User admin) {
        return new RegisterTenantResponse(accessToken, "Bearer", expiresInSeconds,
                TenantResponse.from(tenant), LoginResponse.UserResponse.from(admin));
    }
}
