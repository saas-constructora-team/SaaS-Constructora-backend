package com.SaaS.Constructora.identity.application;

import java.time.LocalDateTime;

/** Datos mínimos que puede ver quien posee el token de una invitación válida. */
public record InvitationPreview(
        String companyName,
        String email,
        LocalDateTime expiresAt
) {
}
