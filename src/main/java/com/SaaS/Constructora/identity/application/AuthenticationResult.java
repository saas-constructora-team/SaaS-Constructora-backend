package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.User;

/** Sesión iniciada devuelta por los casos de uso de login y aceptación de invitación. */
public record AuthenticationResult(
        String accessToken,
        long expiresInSeconds,
        User user,
        Membership membership
) { }
