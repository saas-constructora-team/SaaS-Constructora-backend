package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto (interfaz) que el caso de uso necesita; la implementación vive en infrastructure. */
public interface MembershipRepository {

    Optional<Membership> findActiveByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<Membership> findActiveByUserId(UUID userId);

    boolean existsByUserIdAndTenantIdAndRoleAndActiveTrue(UUID userId, UUID tenantId, Role role);

    void save(Membership membership);

    void deleteByUserIdAndTenantIdAndRoleAndActiveFalse(UUID userId, UUID tenantId, Role role);
}