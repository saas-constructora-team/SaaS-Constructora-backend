package com.SaaS.Constructora.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembershipTest {

    private static Membership membership(Role role, boolean personnelCreationAllowed) {
        return new Membership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                role, true, personnelCreationAllowed, LocalDateTime.now());
    }

    @Test
    void unAdminSiemprePuedeCrearPersonal() {
        assertTrue(membership(Role.ADMIN, false).canCreatePersonnel());
    }

    @Test
    void unMemberSinPermisoNoPuedeCrearPersonal() {
        assertFalse(membership(Role.MEMBER, false).canCreatePersonnel());
    }

    @Test
    void unMemberConPermisoPuedeCrearPersonal() {
        assertTrue(membership(Role.MEMBER, true).canCreatePersonnel());
    }

    @Test
    void elPermisoSeConservaAlCambiarDeRol() {
        Membership updated = membership(Role.MEMBER, true).withRole(Role.ADMIN);
        assertTrue(updated.isPersonnelCreationAllowed());
    }

    @Test
    void unaMembresiaInvitadaNoDaAcceso() {
        Membership invited = Membership.invited(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Role.WORKER, false, LocalDateTime.now());

        assertTrue(invited.isInvited());
        assertFalse(invited.isAccessible());
    }

    @Test
    void activarUnaMembresiaInvitadaLeDaAcceso() {
        Membership activated = Membership.invited(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), Role.WORKER, false, LocalDateTime.now()).activate();

        assertFalse(activated.isInvited());
        assertTrue(activated.isAccessible());
    }

    @Test
    void unaMembresiaActivaPeroDeshabilitadaNoDaAcceso() {
        Membership disabled = new Membership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Role.WORKER, false, LocalDateTime.now());

        assertFalse(disabled.isAccessible());
    }

    @Test
    void volverAInvitarDejaLaMembresiaSinAcceso() {
        Membership reinvited = membership(Role.WORKER, false).reinvited();

        assertTrue(reinvited.isInvited());
        assertFalse(reinvited.isAccessible());
    }
}
