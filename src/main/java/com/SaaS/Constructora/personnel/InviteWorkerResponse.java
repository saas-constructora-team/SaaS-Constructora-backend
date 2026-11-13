package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.identity.application.InvitationIssued;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resultado de invitar (o reenviar la invitación de) un trabajador. */
public record InviteWorkerResponse(
        UUID workerId,
        UUID membershipId,
        UUID userId,
        String email,
        String status,
        LocalDateTime invitationExpiresAt
) {
    public static InviteWorkerResponse from(Worker worker, InvitationIssued issued) {
        return new InviteWorkerResponse(worker.getId(), issued.membershipId(), issued.userId(),
                issued.email(), issued.status().name(), issued.expiresAt());
    }

    public static InviteWorkerResponse from(InvitationIssued issued) {
        return new InviteWorkerResponse(null, issued.membershipId(), issued.userId(),
                issued.email(), issued.status().name(), issued.expiresAt());
    }
}
