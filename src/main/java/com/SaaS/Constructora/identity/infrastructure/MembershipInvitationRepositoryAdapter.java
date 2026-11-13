package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.MembershipInvitationRepository;
import com.SaaS.Constructora.identity.domain.MembershipInvitation;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class MembershipInvitationRepositoryAdapter implements MembershipInvitationRepository {

    private final MembershipInvitationJpaRepository repository;

    MembershipInvitationRepositoryAdapter(MembershipInvitationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public MembershipInvitation save(MembershipInvitation invitation) {
        MembershipInvitationJpaEntity saved = repository.save(new MembershipInvitationJpaEntity(
                invitation.getId(), invitation.getMembershipId(), invitation.getTenantId(),
                invitation.getEmail(), invitation.getInvitedByUserId(), invitation.getTokenHash(),
                invitation.getExpiresAt(), invitation.getAcceptedAt(), invitation.isUsed(),
                invitation.getCreatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<MembershipInvitation> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash)
                .map(MembershipInvitationRepositoryAdapter::toDomain);
    }

    @Override
    public void invalidateActiveForMembership(UUID membershipId) {
        repository.findByMembershipIdAndUsedFalse(membershipId).forEach(entity -> {
            entity.setUsed(true);
            repository.save(entity);
        });
    }

    private static MembershipInvitation toDomain(MembershipInvitationJpaEntity e) {
        return new MembershipInvitation(e.getId(), e.getMembershipId(), e.getTenantId(),
                e.getEmail(), e.getInvitedByUserId(), e.getTokenHash(), e.getExpiresAt(),
                e.getAcceptedAt(), e.isUsed(), e.getCreatedAt());
    }
}
