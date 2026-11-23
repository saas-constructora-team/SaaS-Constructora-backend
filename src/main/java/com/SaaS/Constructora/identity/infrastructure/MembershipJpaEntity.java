package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.MembershipStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "can_create_personnel", nullable = false)
    private boolean canCreatePersonnel;

    @Column(name = "can_view_assignment_candidates", nullable = false)
    private boolean canViewAssignmentCandidates;

    @Column(name = "can_create_tasks", nullable = false)
    private boolean canCreateTasks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected MembershipJpaEntity() { } // requerido por JPA

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, boolean active, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, MembershipStatus.ACTIVE, active, false, false, false, createdAt);
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, boolean active,
                        boolean canCreatePersonnel, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, MembershipStatus.ACTIVE, active,
                canCreatePersonnel, false, false, createdAt);
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, MembershipStatus status,
                        boolean active, boolean canCreatePersonnel, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, status, active,
                canCreatePersonnel, false, false, createdAt);
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, MembershipStatus status,
                        boolean active, boolean canCreatePersonnel,
                        boolean canViewAssignmentCandidates, LocalDateTime createdAt) {
        this(id, userId, tenantId, role, status, active,
                canCreatePersonnel, canViewAssignmentCandidates, false, createdAt);
    }

    MembershipJpaEntity(UUID id, UUID userId, UUID tenantId, Role role, MembershipStatus status,
                        boolean active, boolean canCreatePersonnel,
                        boolean canViewAssignmentCandidates, boolean canCreateTasks,
                        LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.role = role;
        this.status = status;
        this.active = active;
        this.canCreatePersonnel = canCreatePersonnel;
        this.canViewAssignmentCandidates = canViewAssignmentCandidates;
        this.canCreateTasks = canCreateTasks;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getTenantId() { return tenantId; }
    Role getRole() { return role; }
    MembershipStatus getStatus() { return status; }
    boolean isActive() { return active; }
    boolean isCanCreatePersonnel() { return canCreatePersonnel; }
    boolean isCanViewAssignmentCandidates() { return canViewAssignmentCandidates; }
    boolean isCanCreateTasks() { return canCreateTasks; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setActive(boolean active) { this.active = active; }
    void setCanCreatePersonnel(boolean canCreatePersonnel) { this.canCreatePersonnel = canCreatePersonnel; }
    void setCanViewAssignmentCandidates(boolean canViewAssignmentCandidates) { this.canViewAssignmentCandidates = canViewAssignmentCandidates; }
    void setCanCreateTasks(boolean canCreateTasks) { this.canCreateTasks = canCreateTasks; }
}
