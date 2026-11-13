package com.SaaS.Constructora.identity.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MembershipInvitationJpaRepository extends JpaRepository<MembershipInvitationJpaEntity, UUID> {

    Optional<MembershipInvitationJpaEntity> findByTokenHash(String tokenHash);

    List<MembershipInvitationJpaEntity> findByMembershipIdAndUsedFalse(UUID membershipId);
}
