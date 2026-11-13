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

    /** Membresía del usuario en el tenant sin importar su estado (ACTIVE o INVITED). */
    Optional<Membership> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    /** Membresía por id y tenant; usado por la aceptación/reenvío de invitaciones. */
    Optional<Membership> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByUserIdAndTenantIdAndRoleAndActiveTrue(UUID userId, UUID tenantId, Role role);

    void save(Membership membership);

    void deleteByUserIdAndTenantIdAndRoleAndActiveFalse(UUID userId, UUID tenantId, Role role);
}
