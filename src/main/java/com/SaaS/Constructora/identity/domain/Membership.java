package com.SaaS.Constructora.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio que representa la pertenencia de un usuario a un tenant con un rol específico.
 * Es la única fuente de verdad para la relación (user, tenant, role).
 */
public class Membership {

    private final UUID id;
    private final UUID userId;
    private final UUID tenantId;
    private final Role role;
    private final boolean active;
    private final LocalDateTime createdAt;

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}