package com.SaaS.Constructora.identity.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
class TenantJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TenantJpaEntity() { } // requerido por JPA

    TenantJpaEntity(UUID id, String name, String slug, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getSlug() { return slug; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
