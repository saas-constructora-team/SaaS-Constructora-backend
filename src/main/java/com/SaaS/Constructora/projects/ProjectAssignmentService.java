package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso de asignación de personal a proyectos.
 * Toda validación usa el tenant del JWT; proyecto o usuario de otra empresa
 * se reportan como inexistentes (404) para no revelar datos entre tenants.
 * <p>
 * Un {@code ADMIN} del tenant puede gestionar los miembros de cualquier
 * proyecto. Un {@code MEMBER} solo puede hacerlo si fue delegado como
 * {@link ProjectRole#LEAD} en ese proyecto; esa delegación la concede el ADMIN.
 */
@Service
public class ProjectAssignmentService {

    private final ProjectAssignmentRepository repository;
    private final ProjectService projectService;
    private final UserQueryService userQueryService;

    public ProjectAssignmentService(ProjectAssignmentRepository repository,
                                    ProjectService projectService,
                                    UserQueryService userQueryService) {
        this.repository = repository;
        this.projectService = projectService;
        this.userQueryService = userQueryService;
    }

    /**
     * Autoriza la gestión de miembros de un proyecto. Un ADMIN del tenant
     * siempre puede; un MEMBER solo si tiene el rol LEAD en ese proyecto.
     */
    @Transactional(readOnly = true)
    public void requireCanManage(UUID tenantId, UUID projectId, UUID actorId, Role actorRole) {
        projectService.ensureBelongsToTenant(projectId, tenantId);

        if (actorRole.canManageAssignments()) {
            return;
        }

        ProjectAssignment assignment = repository
                .findByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para gestionar los miembros de este proyecto"));

        if (!assignment.getRole().canManageMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para gestionar los miembros de este proyecto");
        }
    }

    /**
     * Agrega un usuario al proyecto con el rol indicado. Un LEAD delegado
     * solo puede agregar MEMBER; nombrar otro LEAD requiere un ADMIN.
     */
    @Transactional
    public ProjectAssignmentResponse assign(UUID tenantId, UUID projectId, UUID userId,
                                            ProjectRole projectRole, UUID actorId, Role actorRole) {
        requireCanManage(tenantId, projectId, actorId, actorRole);
        ProjectRole role = projectRole != null ? projectRole : ProjectRole.MEMBER;
        requireCanGrant(actorRole, role);
        UserSummary user = userQueryService.ensureBelongsToTenant(userId, tenantId);

        if (repository.existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El usuario ya está asignado a este proyecto");
        }

        ProjectAssignment saved = repository.save(
                new ProjectAssignment(tenantId, projectId, userId, role));
        return ProjectAssignmentResponse.from(saved, user);
    }

    @Transactional(readOnly = true)
    public List<ProjectAssignmentResponse> list(UUID tenantId, UUID projectId,
                                                UUID actorId, Role actorRole) {
        requireCanManage(tenantId, projectId, actorId, actorRole);

        Map<UUID, UserSummary> users = userQueryService.listByTenant(tenantId)
                .stream()
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));

        return repository.findByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId)
                .stream()
                .map(assignment -> ProjectAssignmentResponse.from(
                        assignment, users.get(assignment.getUserId())))
                .toList();
    }

    /**
     * Cambia el rol de un usuario dentro del proyecto. Un LEAD solo puede
     * mantener el rol MEMBER: no puede promover a otros ni modificar a otro
     * LEAD; esas operaciones quedan reservadas al ADMIN.
     */
    @Transactional
    public ProjectAssignmentResponse updateRole(UUID tenantId, UUID projectId, UUID userId,
                                                ProjectRole projectRole, UUID actorId,
                                                Role actorRole) {
        requireCanManage(tenantId, projectId, actorId, actorRole);
        if (projectRole == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        requireCanGrant(actorRole, projectRole);
        UserSummary user = userQueryService.ensureBelongsToTenant(userId, tenantId);

        ProjectAssignment assignment = repository
                .findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no está asignado a este proyecto"));

        requireCanModify(actorRole, assignment);

        assignment.changeRole(projectRole);
        return ProjectAssignmentResponse.from(repository.save(assignment), user);
    }

    /**
     * Desasigna un usuario del proyecto. Un LEAD no puede quitar a otro LEAD;
     * esa operación queda reservada al ADMIN. La operación es idempotente:
     * si la asignación ya no existe, igualmente responde 204.
     */
    @Transactional
    public void unassign(UUID tenantId, UUID projectId, UUID userId,
                         UUID actorId, Role actorRole) {
        requireCanManage(tenantId, projectId, actorId, actorRole);
        userQueryService.ensureBelongsToTenant(userId, tenantId);

        if (!actorRole.canManageAssignments()) {
            repository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)
                    .ifPresent(target -> requireCanModify(actorRole, target));
        }

        repository.deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    /** Solo un ADMIN puede conceder roles distintos de MEMBER. */
    private void requireCanGrant(Role actorRole, ProjectRole targetRole) {
        if (!actorRole.canManageAssignments() && targetRole != ProjectRole.MEMBER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede delegar la gestión del proyecto");
        }
    }

    /** Un LEAD no puede modificar ni quitar a otro LEAD. */
    private void requireCanModify(Role actorRole, ProjectAssignment target) {
        if (!actorRole.canManageAssignments() && target.getRole().canManageMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede modificar a un líder del proyecto");
        }
    }
}
