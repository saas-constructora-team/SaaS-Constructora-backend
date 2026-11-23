package com.SaaS.Constructora.personnel.interfaces;
import com.SaaS.Constructora.personnel.infrastructure.Worker;

import com.SaaS.Constructora.identity.application.InvitationIssued;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resultado de invitar (o reenviar la invitación de) un trabajador.
 * <p>
 * {@code emailSent} distingue la invitación creada en base de datos del correo
 * efectivamente entregado. Cuando es {@code false}, {@code emailError} explica
 * el motivo y el cliente puede ofrecer el reenvío de la invitación.
 */
public record InviteWorkerResponse(
        UUID workerId,
        UUID membershipId,
        UUID userId,
        String email,
        String status,
        LocalDateTime invitationExpiresAt,
        boolean emailSent,
        String emailError
) {
    public static InviteWorkerResponse from(Worker worker, InvitationIssued issued) {
        return new InviteWorkerResponse(worker.getId(), issued.membershipId(), issued.userId(),
                issued.email(), issued.status().name(), issued.expiresAt(),
                issued.emailSent(), issued.emailError());
    }

    public static InviteWorkerResponse from(InvitationIssued issued) {
        return new InviteWorkerResponse(null, issued.membershipId(), issued.userId(),
                issued.email(), issued.status().name(), issued.expiresAt(),
                issued.emailSent(), issued.emailError());
    }
}
