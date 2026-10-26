package com.saas.constructora.modules.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio pura para el flujo de recuperación de contraseña (HU-13).
 * El valor del token viaja solo por correo; en base de datos se persiste su hash.
 */
public class PasswordResetToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final boolean used;
    private final LocalDateTime createdAt;

    public PasswordResetToken(UUID id, UUID userId, String tokenHash,
                              LocalDateTime expiresAt, boolean used, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = used;
        this.createdAt = createdAt;
    }

    // Regla de negocio: un token sirve solo si no fue usado y no venció.
    public boolean isValidAt(LocalDateTime now) {
        return !used && expiresAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
