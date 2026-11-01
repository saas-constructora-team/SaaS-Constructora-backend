package com.SaaS.Constructora.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio pura (sin dependencias de JPA).
 * La persistencia vive en {@code identity.infrastructure}.
 * El usuario es una identidad global; el rol y tenant viven en {@link Membership}.
 */
public class User {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final boolean active;
    private final LocalDateTime createdAt;

    public User(UUID id, String email, String fullName,
                String passwordHash, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Regla de negocio: un usuario desactivado no puede autenticarse.
    public boolean canAuthenticate() {
        return active;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
