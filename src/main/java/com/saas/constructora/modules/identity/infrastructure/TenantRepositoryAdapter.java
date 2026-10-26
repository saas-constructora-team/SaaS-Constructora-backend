package com.saas.constructora.modules.identity.infrastructure;

import com.saas.constructora.modules.identity.application.ports.TenantRepository;
import com.saas.constructora.modules.identity.domain.Tenant;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
class TenantRepositoryAdapter implements TenantRepository {

    private final TenantJpaRepository repository;

    TenantRepositoryAdapter(TenantJpaRepository repository) { this.repository = repository; }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return repository.findById(id).map(TenantRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return repository.existsBySlug(slug);
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity saved = repository.save(new TenantJpaEntity(
                tenant.getId(), tenant.getName(), tenant.getSlug(), tenant.getCreatedAt()));
        return toDomain(saved);
    }

    private static Tenant toDomain(TenantJpaEntity e) {
        return new Tenant(e.getId(), e.getName(), e.getSlug(), e.getCreatedAt());
    }
}
