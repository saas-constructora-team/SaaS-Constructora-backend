package com.saas.constructora.modules.identity.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PasswordResetTokenJpaEntity() { } // requerido por JPA

    PasswordResetTokenJpaEntity(UUID id, UUID userId, String tokenHash,
                                LocalDateTime expiresAt, boolean used, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getTokenHash() { return tokenHash; }
    LocalDateTime getExpiresAt() { return expiresAt; }
    boolean isUsed() { return used; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setUsed(boolean used) { this.used = used; }
}
