package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memberships")
class MembershipJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "can_create_personnel", nullable = false)
    private boolean canCreatePersonnel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MembershipJpaEntity() { } // requerido por JPA

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, boolean active, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, active, false, createdAt);
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, boolean active,
                        boolean canCreatePersonnel, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.active = active;
        this.canCreatePersonnel = canCreatePersonnel;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getTenantId() { return tenantId; }
    Role getRole() { return role; }
    boolean isActive() { return active; }
    boolean isCanCreatePersonnel() { return canCreatePersonnel; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setActive(boolean active) { this.active = active; }
    void setCanCreatePersonnel(boolean canCreatePersonnel) { this.canCreatePersonnel = canCreatePersonnel; }
}