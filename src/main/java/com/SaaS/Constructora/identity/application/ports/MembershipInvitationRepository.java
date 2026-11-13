package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.MembershipInvitation;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de invitaciones a membresías (flujo invite-and-claim). */
public interface MembershipInvitationRepository {

    MembershipInvitation save(MembershipInvitation invitation);

    Optional<MembershipInvitation> findByTokenHash(String tokenHash);

    /** Marca como usadas las invitaciones vigentes previas de una membresía. */
    void invalidateActiveForMembership(UUID membershipId);
}
