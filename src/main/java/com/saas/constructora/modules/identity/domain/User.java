package com.saas.constructora.modules.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio pura (sin dependencias de JPA).
 * La persistencia vive en {@code identity.infrastructure}.
 */
public class User {

    private final UUID id;
    private final UUID tenantId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final LocalDateTime createdAt;

    public User(UUID id, UUID tenantId, String email, String fullName,
                String passwordHash, Role role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    // Regla de negocio: un usuario desactivado no puede autenticarse.
    public boolean canAuthenticate() {
        return active;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
