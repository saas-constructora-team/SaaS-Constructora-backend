package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.MembershipStatus;
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
        return repository.findActiveByUserIdAndTenantId(userId, tenantId, MembershipStatus.ACTIVE)
                .map(this::toDomain);
    }

    @Override
    public List<Membership> findActiveByUserId(UUID userId) {
        return repository.findActiveByUserId(userId, MembershipStatus.ACTIVE)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId) {
        return repository.findByUserIdAndTenantId(userId, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<Membership> findByIdAndTenantId(UUID id, UUID tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndTenantIdAndRoleAndActiveTrue(UUID userId, UUID tenantId, Role role) {
        return repository.existsByUserIdAndTenantIdAndRoleAndActiveTrue(userId, tenantId, role);
    }

    @Override
    public void save(Membership membership) {
        repository.save(new MembershipJpaEntity(
                membership.getId(), membership.getUserId(), membership.getTenantId(),
                membership.getRole(), membership.getStatus(), membership.isActive(),
                membership.isPersonnelCreationAllowed(),
                membership.isAssignmentCandidatesViewAllowed(),
                membership.isTaskCreationAllowed(),
                membership.isSiteLogManagementAllowed(), membership.getCreatedAt()));
    }

    @Override
    public void deleteByUserIdAndTenantIdAndRoleAndActiveFalse(UUID userId, UUID tenantId, Role role) {
        repository.deleteByUserIdAndTenantIdAndRoleAndActiveFalse(userId, tenantId, role);
    }

    private Membership toDomain(MembershipJpaEntity e) {
        return new Membership(e.getId(), e.getUserId(), e.getTenantId(), e.getRole(),
                e.getStatus(), e.isActive(), e.isCanCreatePersonnel(),
                e.isCanViewAssignmentCandidates(), e.isCanCreateTasks(),
                e.isCanManageSiteLog(), e.getCreatedAt());
    }
}
