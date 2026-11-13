package com.SaaS.Constructora.identity.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "membership_invitations")
class MembershipInvitationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "membership_id", nullable = false, updatable = false)
    private UUID membershipId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "invited_by_user_id", nullable = false, updatable = false)
    private UUID invitedByUserId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MembershipInvitationJpaEntity() { } // requerido por JPA

    MembershipInvitationJpaEntity(UUID id, UUID membershipId, UUID tenantId, String email,
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

    UUID getId() { return id; }
    UUID getMembershipId() { return membershipId; }
    UUID getTenantId() { return tenantId; }
    String getEmail() { return email; }
    UUID getInvitedByUserId() { return invitedByUserId; }
    String getTokenHash() { return tokenHash; }
    LocalDateTime getExpiresAt() { return expiresAt; }
    LocalDateTime getAcceptedAt() { return acceptedAt; }
    boolean isUsed() { return used; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setUsed(boolean used) { this.used = used; }
    void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}
