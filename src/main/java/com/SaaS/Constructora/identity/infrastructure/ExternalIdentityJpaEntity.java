package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.domain.ExternalIdentity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_identities",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_external_identity", columnNames = {"provider", "provider_user_id"}))
class ExternalIdentityJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ExternalIdentityJpaEntity() { } // requerido por JPA

    ExternalIdentityJpaEntity(UUID id, UUID userId, String provider,
                              String providerUserId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    String getProvider() { return provider; }
    String getProviderUserId() { return providerUserId; }
    LocalDateTime getCreatedAt() { return createdAt; }
}
