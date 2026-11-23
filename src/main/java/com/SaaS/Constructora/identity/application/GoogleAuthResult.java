package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;

/** Sesión iniciada tras autenticar con Google. */
public record GoogleAuthResult(
        String accessToken,
        long expiresInSeconds,
        boolean newUser,
        Tenant tenant,
        User user,
        Membership membership
) { }
