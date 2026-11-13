package com.SaaS.Constructora.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Invitación para que una persona active su membresía dentro de una empresa.
 * <p>
 * El token en claro solo viaja en el correo; en base de datos se persiste su
 * hash (ver {@code TokenHasher}). La invitación siempre apunta a una membresía
 * en estado {@link MembershipStatus#INVITED}.
 */
public class MembershipInvitation {

    private final UUID id;
    private final UUID membershipId;
    private final UUID tenantId;
    private final String email;
    private final UUID invitedByUserId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime acceptedAt;
    private final boolean used;
    private final LocalDateTime createdAt;

    public MembershipInvitation(UUID id, UUID membershipId, UUID tenantId, String email,
                                UUID invitedByUserId, String tokenHash, LocalDateTime expiresAt,
                                LocalDateTime acceptedAt, boolean used, LocalDateTime createdAt) {
        this.id = id;
        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.email = email;
        this.invitedByUserId = invitedByUserId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    /** Regla de negocio: una invitación sirve solo si no fue usada ni venció. */
    public boolean isUsable(LocalDateTime now) {
        return !used && acceptedAt == null && expiresAt.isAfter(now);
    }

    /** Devuelve una copia marcada como usada por haber sido aceptada. */
    public MembershipInvitation acceptedAt(LocalDateTime acceptedAt) {
        return new MembershipInvitation(id, membershipId, tenantId, email, invitedByUserId,
                tokenHash, expiresAt, acceptedAt, true, createdAt);
    }

    /** Devuelve una copia marcada como usada sin aceptación (reenvío o revocación). */
    public MembershipInvitation invalidated() {
        return new MembershipInvitation(id, membershipId, tenantId, email, invitedByUserId,
                tokenHash, expiresAt, acceptedAt, true, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getMembershipId() { return membershipId; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public UUID getInvitedByUserId() { return invitedByUserId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public boolean isUsed() { return used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
