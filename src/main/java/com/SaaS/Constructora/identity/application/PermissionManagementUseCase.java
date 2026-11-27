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
 * entre ADMIN y MEMBER, o concediendo el permiso puntual de creación de
 * personal a un MEMBER. El tenant siempre proviene del token del solicitante.
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

    /** Habilita a un MEMBER para crear personal de la empresa. */
    @Transactional
    public UserSummary enablePersonnelCreation(UUID tenantId, UUID userId) {
        return changePersonnelCreation(tenantId, userId, true);
    }

    /** Revoca el permiso de creación de personal de un MEMBER. */
    @Transactional
    public UserSummary disablePersonnelCreation(UUID tenantId, UUID userId) {
        return changePersonnelCreation(tenantId, userId, false);
    }

    /** Habilita a un MEMBER para consultar candidatos a asignación de proyectos. */
    @Transactional
    public UserSummary enableAssignmentCandidatesView(UUID tenantId, UUID userId) {
        return changeAssignmentCandidatesView(tenantId, userId, true);
    }

    /** Revoca el permiso de consultar candidatos a asignación de un MEMBER. */
    @Transactional
    public UserSummary disableAssignmentCandidatesView(UUID tenantId, UUID userId) {
        return changeAssignmentCandidatesView(tenantId, userId, false);
    }

    /** Habilita a un WORKER o MEMBER para crear tareas en los proyectos asignados. */
    @Transactional
    public UserSummary enableTaskCreation(UUID tenantId, UUID userId) {
        return changeTaskCreation(tenantId, userId, true);
    }

    /** Revoca el permiso de creación de tareas del usuario indicado. */
    @Transactional
    public UserSummary disableTaskCreation(UUID tenantId, UUID userId) {
        return changeTaskCreation(tenantId, userId, false);
    }

    /** Habilita a un WORKER o MEMBER para editar/eliminar entradas de la bitácora. */
    @Transactional
    public UserSummary enableSiteLogManagement(UUID tenantId, UUID userId) {
        return changeSiteLogManagement(tenantId, userId, true);
    }

    /** Revoca el permiso de gestionar la bitácora del usuario indicado. */
    @Transactional
    public UserSummary disableSiteLogManagement(UUID tenantId, UUID userId) {
        return changeSiteLogManagement(tenantId, userId, false);
    }

    private UserSummary changeRole(UUID tenantId, UUID userId, Role targetRole) {
        // Valida que el usuario exista y pertenezca al tenant del solicitante.
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);

        Membership membership = requireMembership(tenantId, userId);

        // La operación es idempotente: si ya tiene el rol pedido no se persiste nada.
        if (membership.getRole() != targetRole) {
            membership = membership.withRole(targetRole);
            membershipRepository.save(membership);
        }

        return summaryOf(target, membership);
    }

    private UserSummary changePersonnelCreation(UUID tenantId, UUID userId, boolean allowed) {
        // Valida que el usuario exista y pertenezca al tenant del solicitante.
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);

        Membership membership = requireMembership(tenantId, userId);

        // La operación es idempotente: si el permiso ya tiene el valor pedido no se persiste nada.
        if (membership.isPersonnelCreationAllowed() != allowed) {
            membership = membership.withPersonnelCreationPermission(allowed);
            membershipRepository.save(membership);
        }

        return summaryOf(target, membership);
    }

    private UserSummary changeAssignmentCandidatesView(UUID tenantId, UUID userId, boolean allowed) {
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);
        Membership membership = requireMembership(tenantId, userId);

        if (membership.isAssignmentCandidatesViewAllowed() != allowed) {
            membership = membership.withAssignmentCandidatesViewPermission(allowed);
            membershipRepository.save(membership);
        }

        return summaryOf(target, membership);
    }

    private UserSummary changeTaskCreation(UUID tenantId, UUID userId, boolean allowed) {
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);
        Membership membership = requireMembership(tenantId, userId);

        if (membership.isTaskCreationAllowed() != allowed) {
            membership = membership.withTaskCreationPermission(allowed);
            membershipRepository.save(membership);
        }

        return summaryOf(target, membership);
    }

    private UserSummary changeSiteLogManagement(UUID tenantId, UUID userId, boolean allowed) {
        UserSummary target = userQueryService.ensureBelongsToTenant(userId, tenantId);
        Membership membership = requireMembership(tenantId, userId);

        if (membership.isSiteLogManagementAllowed() != allowed) {
            membership = membership.withSiteLogManagementPermission(allowed);
            membershipRepository.save(membership);
        }

        return summaryOf(target, membership);
    }

    private Membership requireMembership(UUID tenantId, UUID userId) {
        return membershipRepository.findActiveByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private static UserSummary summaryOf(UserSummary target, Membership membership) {
        return new UserSummary(target.id(), target.email(), target.fullName(),
                membership.getRole(), target.active(), membership.canCreatePersonnel(),
                membership.canViewAssignmentCandidates(), membership.canCreateTasks(),
                membership.canManageSiteLog());
    }
}
