package com.SaaS.Constructora.shared.events;

import java.time.LocalDateTime;
import java.util.UUID;

/** La membresía dejó de ser accesible: sus asignaciones deben retirarse. */
public record MembershipRevoked(
        UUID eventId,
        UUID tenantId,
        UUID membershipId,
        UUID userId,
        UUID actorUserId,
        LocalDateTime occurredAt) {
}
