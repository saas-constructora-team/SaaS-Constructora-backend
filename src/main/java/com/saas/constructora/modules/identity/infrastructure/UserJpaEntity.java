package com.saas.constructora.modules.identity.infrastructure;

import com.saas.constructora.modules.identity.domain.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserJpaEntity() { } // requerido por JPA

    UserJpaEntity(UUID id, UUID tenantId, String email, String fullName,
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

    UUID getId() { return id; }
    UUID getTenantId() { return tenantId; }
    String getEmail() { return email; }
    String getFullName() { return fullName; }
    String getPasswordHash() { return passwordHash; }
    Role getRole() { return role; }
    boolean isActive() { return active; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
