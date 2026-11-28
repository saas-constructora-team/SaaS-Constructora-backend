package com.SaaS.Constructora.identity.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 30)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserJpaEntity() { } // requerido por JPA

    UserJpaEntity(UUID id, String email, String fullName, String username,
                  String passwordHash, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getEmail() { return email; }
    String getFullName() { return fullName; }
    String getUsername() { return username; }
    String getPasswordHash() { return passwordHash; }
    boolean isActive() { return active; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void setFullName(String fullName) { this.fullName = fullName; }
    void setUsername(String username) { this.username = username; }
    void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    void setActive(boolean active) { this.active = active; }
}
