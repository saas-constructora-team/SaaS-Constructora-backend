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
    private final MembershipStatus status;
    private final boolean active;
    private final boolean personnelCreationAllowed;
    private final boolean assignmentCandidatesViewAllowed;
    private final LocalDateTime createdAt;

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, boolean active, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, MembershipStatus.ACTIVE, active, false, false, createdAt);
    }

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, boolean active,
                      boolean personnelCreationAllowed, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, MembershipStatus.ACTIVE, active,
                personnelCreationAllowed, false, createdAt);
    }

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, MembershipStatus status,
                      boolean active, boolean personnelCreationAllowed, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, status, active, personnelCreationAllowed, false, createdAt);
    }

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, MembershipStatus status,
                      boolean active, boolean personnelCreationAllowed,
                      boolean assignmentCandidatesViewAllowed, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.status = status;
        this.active = active;
        this.personnelCreationAllowed = personnelCreationAllowed;
        this.assignmentCandidatesViewAllowed = assignmentCandidatesViewAllowed;
        this.createdAt = createdAt;
    }

    /**
     * Crea una membresía pendiente de aceptación. La invitación se emite por
     * separado y el usuario todavía no puede autenticarse en este tenant.
     */
    public static Membership invited(UUID id, UUID userId, UUID tenantId, Role role,
                                     boolean personnelCreationAllowed, LocalDateTime createdAt) {
        return invited(id, userId, tenantId, role, personnelCreationAllowed, false, createdAt);
    }

    public static Membership invited(UUID id, UUID userId, UUID tenantId, Role role,
                                     boolean personnelCreationAllowed,
                                     boolean assignmentCandidatesViewAllowed,
                                     LocalDateTime createdAt) {
        return new Membership(id, userId, tenantId, role, MembershipStatus.INVITED,
                true, personnelCreationAllowed, assignmentCandidatesViewAllowed, createdAt);
    }

    /**
     * Devuelve una copia de esta membresía con el rol indicado.
     * La entidad es inmutable: los cambios de permisos crean una nueva instancia.
     */
    public Membership withRole(Role newRole) {
        return new Membership(id, userId, tenantId, newRole, status, active,
                personnelCreationAllowed, assignmentCandidatesViewAllowed, createdAt);
    }

    /** Devuelve una copia con el permiso de creación de personal actualizado. */
    public Membership withPersonnelCreationPermission(boolean allowed) {
        return new Membership(id, userId, tenantId, role, status, active, allowed,
                assignmentCandidatesViewAllowed, createdAt);
    }

    /** Devuelve una copia con el permiso de consultar candidatos a asignación actualizado. */
    public Membership withAssignmentCandidatesViewPermission(boolean allowed) {
        return new Membership(id, userId, tenantId, role, status, active,
                personnelCreationAllowed, allowed, createdAt);
    }

    /** Devuelve una copia lista para volver a invitar a una membresía no activa. */
    public Membership reinvited() {
        return new Membership(id, userId, tenantId, role, MembershipStatus.INVITED,
                true, personnelCreationAllowed, assignmentCandidatesViewAllowed, createdAt);
    }

    /** Activa la membresía cuando la persona acepta la invitación. */
    public Membership activate() {
        return new Membership(id, userId, tenantId, role, MembershipStatus.ACTIVE,
                true, personnelCreationAllowed, assignmentCandidatesViewAllowed, createdAt);
    }

    /**
     * Una membresía solo da acceso si está activa y su estado es ACTIVE.
     * Las membresías INVITED quedan explícitamente fuera del login y del filtro
     * de validación de tenant.
     */
    public boolean isAccessible() {
        return status == MembershipStatus.ACTIVE && active;
    }

    public boolean isInvited() {
        return status == MembershipStatus.INVITED;
    }

    /**
     * Permiso efectivo para crear personal: un ADMIN siempre puede; un MEMBER
     * solo si un ADMIN le habilitó el permiso de forma explícita.
     */
    public boolean canCreatePersonnel() {
        return role.canCreatePersonnel() || personnelCreationAllowed;
    }

    /**
     * Permiso efectivo para consultar candidatos a asignación de proyectos:
     * un ADMIN siempre puede; un MEMBER solo si un ADMIN le habilitó el
     * permiso de forma explícita.
     */
    public boolean canViewAssignmentCandidates() {
        return role.canViewAssignmentCandidates() || assignmentCandidatesViewAllowed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public Role getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
    public boolean isActive() { return active; }
    public boolean isPersonnelCreationAllowed() { return personnelCreationAllowed; }
    public boolean isAssignmentCandidatesViewAllowed() { return assignmentCandidatesViewAllowed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
