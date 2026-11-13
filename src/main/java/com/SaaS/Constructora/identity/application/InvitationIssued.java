package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.domain.MembershipStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado interno de emitir una invitación. La capa HTTP decide qué campos
 * expone; el token en claro nunca sale de este objeto (viaja solo por correo).
 */
public record InvitationIssued(
        UUID membershipId,
        UUID userId,
        String email,
        String fullName,
        String companyName,
        MembershipStatus status,
        LocalDateTime expiresAt
) {
}
