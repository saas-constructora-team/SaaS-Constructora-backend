package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;

public record AccountProfile(User user, Membership membership, Tenant tenant, boolean tenantOwner) {
    public static AccountProfile from(User user, Membership membership, Tenant tenant) {
        return new AccountProfile(user, membership, tenant, tenant.isOwnedBy(user.getId()));
    }
}
