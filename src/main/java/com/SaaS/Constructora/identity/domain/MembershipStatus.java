package com.SaaS.Constructora.identity.domain;

/**
 * Estado de una membresía (relación usuario-empresa).
 * <p>
 * Una membresía {@link #INVITED} no permite autenticarse ni operar dentro de la
 * empresa: solo existe para reservar el vínculo y emitir la invitación. Pasa a
 * {@link #ACTIVE} cuando la persona acepta la invitación.
 */
public enum MembershipStatus {
    INVITED,
    ACTIVE
}
