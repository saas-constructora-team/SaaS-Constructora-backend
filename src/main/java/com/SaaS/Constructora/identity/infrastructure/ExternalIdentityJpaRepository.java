package com.SaaS.Constructora.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

interface ExternalIdentityJpaRepository extends JpaRepository<ExternalIdentityJpaEntity, UUID> {
    Optional<ExternalIdentityJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
