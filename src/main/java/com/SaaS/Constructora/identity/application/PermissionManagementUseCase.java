package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Caso de uso de administración de permisos dentro de una empresa.
 * Un ADMIN habilita o deshabilita los permisos de un usuario cambiando su rol
 * entre ADMIN y MEMBER. El tenant siempre proviene del token del solicitante.
 */
@Service
public class PermissionManagementUseCase {

    private final MembershipRepository membershipRepository;
    private final UserQueryService userQueryService;

    public PermissionManagementUseCase(MembershipRepository membershipRepository,
                                       UserQueryService userQueryService) {
        this.membershipRepository = membershipRepository;
        this.userQueryService = userQueryService;
    }

    /** Habilita los permisos de administración del usuario indicado. */
    @Transactional
    public UserSummary enable(UUID tenantId, UUID userId) {
        return changeRole(tenantId, userId, Role.ADMIN);
    }

    /** Deshabilita los permisos de administración del usuario indicado. */
    @Transactional
    public UserSummary disable(UUID tenantId, UUID userId) {
        return changeRole(tenantId, userId, Role.MEMBER);
    }

    private UserSummary changeRole(UUID tenantId, UUID userId, Role targetRole) {
        // Valida que el usuario exista y pertenezca al tenant del solicitante.
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);

        Membership membership = membershipRepository
                .findActiveByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // La operación es idempotente: si ya tiene el rol pedido no se persiste nada.
        if (membership.getRole() != targetRole) {
            membershipRepository.save(membership.withRole(targetRole));
        }

        return new UserSummary(target.id(), target.email(), target.fullName(),
                targetRole, target.active());
    }
}
