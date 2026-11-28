package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.domain.Tenant;

import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String slug,
        String tradeName,
        String taxId,
        String contactPhone,
        String address,
        String companyEmail,
        UUID ownerUserId
) {
    public static CompanyResponse from(Tenant tenant) {
        return new CompanyResponse(tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getTradeName(),
                tenant.getTaxId(), tenant.getContactPhone(), tenant.getAddress(), tenant.getCompanyEmail(),
                tenant.getOwnerUserId());
    }
}
