package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class MembershipRepositoryAdapter implements MembershipRepository {

    private final MembershipJpaRepository repository;

    MembershipRepositoryAdapter(MembershipJpaRepository repository) { this.repository = repository; }

    @Override
    public Optional<Membership> findActiveByUserIdAndTenantId(UUID userId, UUID tenantId) {
        return repository.findActiveByUserIdAndTenantId(userId, tenantId).map(this::toDomain);
    }

    @Override
    public List<Membership> findActiveByUserId(UUID userId) {
        return repository.findActiveByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByUserIdAndTenantIdAndRoleAndActiveTrue(UUID userId, UUID tenantId, Role role) {
        return repository.existsByUserIdAndTenantIdAndRoleAndActiveTrue(userId, tenantId, role);
    }

    @Override
    public void save(Membership membership) {
        repository.save(new MembershipJpaEntity(
                membership.getId(), membership.getUserId(), membership.getTenantId(),
                membership.getRole(), membership.isActive(), membership.getCreatedAt()));
    }

    @Override
    public void deleteByUserIdAndTenantIdAndRoleAndActiveFalse(UUID userId, UUID tenantId, Role role) {
        repository.deleteByUserIdAndTenantIdAndRoleAndActiveFalse(userId, tenantId, role);
    }

    private Membership toDomain(MembershipJpaEntity e) {
        return new Membership(e.getId(), e.getUserId(), e.getTenantId(), e.getRole(), e.isActive(), e.getCreatedAt());
    }
}