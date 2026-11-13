package com.SaaS.Constructora.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipInvitationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 10, 0);

    private static MembershipInvitation invitation(LocalDateTime expiresAt,
                                                   LocalDateTime acceptedAt, boolean used) {
        return new MembershipInvitation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "obrero@demo.com", UUID.randomUUID(), "hash", expiresAt, acceptedAt, used, NOW);
    }

    @Test
    void unaInvitacionVigenteEsUsable() {
        assertTrue(invitation(NOW.plusHours(1), null, false).isUsable(NOW));
    }

    @Test
    void unaInvitacionVencidaNoEsUsable() {
        assertFalse(invitation(NOW.minusSeconds(1), null, false).isUsable(NOW));
    }

    @Test
    void unaInvitacionYaUsadaNoEsUsable() {
        assertFalse(invitation(NOW.plusHours(1), null, true).isUsable(NOW));
    }

    @Test
    void unaInvitacionAceptadaNoEsUsable() {
        assertFalse(invitation(NOW.plusHours(1), NOW, false).isUsable(NOW));
    }

    @Test
    void aceptarMarcaLaInvitacionComoUsada() {
        MembershipInvitation accepted = invitation(NOW.plusHours(1), null, false).acceptedAt(NOW);

        assertTrue(accepted.isUsed());
        assertFalse(accepted.isUsable(NOW.plusHours(2)));
    }

    @Test
    void invalidarMarcaLaInvitacionComoUsada() {
        MembershipInvitation invalidated = invitation(NOW.plusHours(1), null, false).invalidated();

        assertTrue(invalidated.isUsed());
        assertFalse(invalidated.isUsable(NOW));
    }
}
