package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;

import java.util.UUID;

public record AccountProfileResponse(
        UserProfileResponse user,
        CompanyResponse company,
        boolean tenantOwner,
        boolean canEditCompany
) {
    public record UserProfileResponse(UUID id, UUID tenantId, String email, String fullName, String username, String role) {
        static UserProfileResponse from(User user, Membership membership) {
            return new UserProfileResponse(user.getId(), membership.getTenantId(), user.getEmail(),
                    user.getFullName(), user.getUsername(), membership.getRole().name());
        }
    }

    public static AccountProfileResponse from(User user, Membership membership, Tenant tenant) {
        boolean owner = tenant.isOwnedBy(user.getId());
        return new AccountProfileResponse(UserProfileResponse.from(user, membership),
                CompanyResponse.from(tenant), owner, owner);
    }
}
