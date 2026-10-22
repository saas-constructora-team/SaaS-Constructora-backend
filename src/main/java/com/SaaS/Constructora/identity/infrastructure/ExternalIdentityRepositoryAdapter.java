package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.ExternalIdentityRepository;
import com.SaaS.Constructora.identity.domain.ExternalIdentity;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
class ExternalIdentityRepositoryAdapter implements ExternalIdentityRepository {

    private final ExternalIdentityJpaRepository repository;

    ExternalIdentityRepositoryAdapter(ExternalIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ExternalIdentity> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return repository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(ExternalIdentityRepositoryAdapter::toDomain);
    }

    @Override
    public ExternalIdentity save(ExternalIdentity identity) {
        ExternalIdentityJpaEntity saved = repository.save(new ExternalIdentityJpaEntity(
                identity.getId(), identity.getUserId(), identity.getProvider(),
                identity.getProviderUserId(), identity.getCreatedAt()));
        return toDomain(saved);
    }

    private static ExternalIdentity toDomain(ExternalIdentityJpaEntity e) {
        return new ExternalIdentity(e.getId(), e.getUserId(), e.getProvider(),
                e.getProviderUserId(), e.getCreatedAt());
    }
}
