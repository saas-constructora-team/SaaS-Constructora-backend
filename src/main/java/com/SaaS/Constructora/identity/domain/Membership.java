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
    private final boolean personnelCreationAllowed;
    private final LocalDateTime createdAt;

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, boolean active, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, active, false, createdAt);
    }

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, boolean active,
                      boolean personnelCreationAllowed, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.active = active;
        this.personnelCreationAllowed = personnelCreationAllowed;
        this.createdAt = createdAt;
    }

    /**
     * Devuelve una copia de esta membresía con el rol indicado.
     * La entidad es inmutable: los cambios de permisos crean una nueva instancia.
     */
    public Membership withRole(Role newRole) {
        return new Membership(id, userId, tenantId, newRole, active,
                personnelCreationAllowed, createdAt);
    }

    /** Devuelve una copia con el permiso de creación de personal actualizado. */
    public Membership withPersonnelCreationPermission(boolean allowed) {
        return new Membership(id, userId, tenantId, role, active, allowed, createdAt);
    }

    /**
     * Permiso efectivo para crear personal: un ADMIN siempre puede; un MEMBER
     * solo si un ADMIN le habilitó el permiso de forma explícita.
     */
    public boolean canCreatePersonnel() {
        return role.canCreatePersonnel() || personnelCreationAllowed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isPersonnelCreationAllowed() { return personnelCreationAllowed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}