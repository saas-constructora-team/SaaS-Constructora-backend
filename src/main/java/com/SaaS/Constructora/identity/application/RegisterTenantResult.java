package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;

/** Sesión iniciada tras registrar una empresa y su administrador. */
public record RegisterTenantResult(
        String accessToken,
        long expiresInSeconds,
        Tenant tenant,
        User admin,
        Membership membership
) { }
