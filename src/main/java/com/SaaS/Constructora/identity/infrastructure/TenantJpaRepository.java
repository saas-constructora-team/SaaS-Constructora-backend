package com.SaaS.Constructora.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface TenantJpaRepository extends JpaRepository<TenantJpaEntity, UUID> {
    boolean existsBySlug(String slug);
}
