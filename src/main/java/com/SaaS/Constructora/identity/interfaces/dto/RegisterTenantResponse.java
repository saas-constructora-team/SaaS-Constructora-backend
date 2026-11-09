package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.domain.Membership;
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
    public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String tradeName,
        String taxId,
        String contactPhone,
        String address,
        String companyEmail
    ) {
        /** Compatibilidad: respuestas que solo necesitan la identificación básica del tenant. */
        public TenantResponse(UUID id, String name, String slug) {
            this(id, name, slug, null, null, null, null, null);
        }

        static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getName(), t.getSlug(), t.getTradeName(),
                    t.getTaxId(), t.getContactPhone(), t.getAddress(), t.getCompanyEmail());
        }
    }

    public static RegisterTenantResponse from(String accessToken, long expiresInSeconds,
                                              Tenant tenant, User admin, Membership membership) {
        return new RegisterTenantResponse(accessToken, "Bearer", expiresInSeconds,
                TenantResponse.from(tenant), LoginResponse.UserResponse.from(admin, membership));
    }
}
