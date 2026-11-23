package com.SaaS.Constructora.projects.application;

import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import com.SaaS.Constructora.projects.domain.ProjectRole;
import com.SaaS.Constructora.projects.interfaces.ProjectAssignmentResponse;
import com.SaaS.Constructora.shared.events.ProjectAssignmentChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso de asignación de personal a proyectos.
 * Toda validación usa el tenant del JWT; proyecto o usuario de otra empresa
 * se reportan como inexistentes (404) para no revelar datos entre tenants.
 * <p>
 * La autoridad del actor se resuelve contra la membresía activa en BD (no contra
 * el claim del JWT, que puede quedar obsoleto). Un ADMIN del tenant puede
 * gestionar cualquier proyecto; un MEMBER solo si fue delegado como LEAD.
 */
@Service
public class ProjectAssignmentService {

    private final ProjectAssignmentRepository repository;
    private final ProjectService projectService;
    private final UserQueryService userQueryService;
    private final ApplicationEventPublisher events;

    public ProjectAssignmentService(ProjectAssignmentRepository repository,
                                    ProjectService projectService,
                                    UserQueryService userQueryService,
                                    ApplicationEventPublisher events) {
        this.repository = repository;
        this.projectService = projectService;
        this.userQueryService = userQueryService;
        this.events = events;
    }

    /**
     * Autoriza la gestión de miembros de un proyecto y devuelve la autoridad
     * vigente del actor. ADMIN siempre puede; MEMBER solo si es LEAD del proyecto.
     */
    @Transactional(readOnly = true)
    public UserSummary requireCanManage(UUID tenantId, UUID projectId, UUID actorId) {
        projectService.ensureBelongsToTenant(projectId, tenantId);

        UserSummary actor = userQueryService.ensureBelongsToTenant(actorId, tenantId);
        if (actor.canManageAssignments()) {
            return actor;
        }

        ProjectAssignment assignment = repository
                .findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No tiene permisos para gestionar los miembros de este proyecto"));

        if (!assignment.getRole().canManageMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tiene permisos para gestionar los miembros de este proyecto");
        }
        return actor;
    }

    /**
     * Agrega un usuario al proyecto con el rol indicado. Un LEAD delegado
     * solo puede agregar MEMBER; nombrar otro LEAD requiere un ADMIN.
     */
    @Transactional
    public ProjectAssignmentResponse assign(UUID tenantId, UUID projectId, UUID userId,
                                            ProjectRole projectRole, UUID actorId) {
        UserSummary actor = requireCanManage(tenantId, projectId, actorId);
        ProjectRole role = projectRole != null ? projectRole : ProjectRole.MEMBER;
        requireCanGrant(actor, role);
        UserSummary user = userQueryService.ensureBelongsToTenant(userId, tenantId);

        if (repository.existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El usuario ya está asignado a este proyecto");
        }

        ProjectAssignment saved = repository.save(
                new ProjectAssignment(tenantId, projectId, userId, role, actorId));
        publish(saved, actorId, "ProjectAssignmentAssigned",
                "Asignado " + user.fullName() + " como " + role);
        return ProjectAssignmentResponse.from(saved, user);
    }

    @Transactional(readOnly = true)
    public List<ProjectAssignmentResponse> list(UUID tenantId, UUID projectId, UUID actorId) {
        requireCanManage(tenantId, projectId, actorId);

        List<ProjectAssignment> assignments =
                repository.findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId);
        Set<UUID> userIds = assignments.stream()
                .map(ProjectAssignment::getUserId)
                .collect(Collectors.toSet());
        // Una sola consulta para enriquecer solo los usuarios asignados.
        Map<UUID, UserSummary> users = userIds.isEmpty() ? Map.of()
                : userQueryService.listByTenant(tenantId, userIds).stream()
                        .collect(Collectors.toMap(UserSummary::id, Function.identity()));

        return assignments.stream()
                .map(assignment -> ProjectAssignmentResponse.from(
                        assignment, users.get(assignment.getUserId())))
                .toList();
    }

    /**
     * Cambia el rol de un usuario dentro del proyecto. Un LEAD solo puede
     * mantener el rol MEMBER: no puede promover a otros ni modificar a otro LEAD.
     */
    @Transactional
    public ProjectAssignmentResponse updateRole(UUID tenantId, UUID projectId, UUID userId,
                                                ProjectRole projectRole, UUID actorId) {
        UserSummary actor = requireCanManage(tenantId, projectId, actorId);
        if (projectRole == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }

        requireCanGrant(actor, projectRole);
        UserSummary user = userQueryService.ensureBelongsToTenant(userId, tenantId);

        ProjectAssignment assignment = repository
                .findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no está asignado a este proyecto"));

        requireCanModify(actor, assignment);

        assignment.changeRole(projectRole);
        ProjectAssignment saved = repository.save(assignment);
        publish(saved, actorId, "ProjectAssignmentRoleChanged",
                "Rol de " + user.fullName() + " cambiado a " + projectRole);
        return ProjectAssignmentResponse.from(saved, user);
    }

    /**
     * Retira a un usuario del proyecto con borrado lógico. Idempotente: si ya
     * no estaba asignado, responde 204 sin cambios.
     */
    @Transactional
    public void unassign(UUID tenantId, UUID projectId, UUID userId, UUID actorId) {
        UserSummary actor = requireCanManage(tenantId, projectId, actorId);
        var target = repository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
        if (!actor.canManageAssignments()) {
            target.ifPresent(assignment -> requireCanModify(actor, assignment));
        }

        LocalDateTime now = LocalDateTime.now();
        int affected = repository.unassign(tenantId, projectId, userId, actorId, now);
        if (affected > 0) {
            publish(target.map(ProjectAssignment::getId).orElse(UUID.randomUUID()),
                    tenantId, projectId, userId, actorId,
                    "ProjectAssignmentUnassigned", "Asignación retirada", now);
        }
    }

    /**
     * Retira todas las asignaciones de un usuario al perder su membresía.
     * No exige permisos del actor: es un efecto de un evento de identity.
     */
    @Transactional
    public void unassignAllForUser(UUID tenantId, UUID userId, UUID actorId) {
        LocalDateTime now = LocalDateTime.now();
        List<ProjectAssignment> assignments =
                repository.findActiveByTenantIdAndUserId(tenantId, userId);
        repository.unassignAllForUser(tenantId, userId, actorId, now);
        assignments.forEach(assignment -> publish(
                assignment.getId(), tenantId, assignment.getProjectId(), userId, actorId,
                "ProjectAssignmentUnassigned", "Asignación retirada por revocación de membresía", now));
    }

    /** Solo un ADMIN puede conceder roles distintos de MEMBER. */
    private void requireCanGrant(UserSummary actor, ProjectRole targetRole) {
        if (!actor.canManageAssignments() && targetRole != ProjectRole.MEMBER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede delegar la gestión del proyecto");
        }
    }

    /** Un LEAD no puede modificar ni quitar a otro LEAD. */
    private void requireCanModify(UserSummary actor, ProjectAssignment target) {
        if (!actor.canManageAssignments() && target.getRole().canManageMembers()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un administrador puede modificar a un líder del proyecto");
        }
    }

    private void publish(ProjectAssignment assignment, UUID actorId, String eventType, String summary) {
        publish(assignment.getId(), assignment.getTenantId(), assignment.getProjectId(),
                assignment.getUserId(), actorId, eventType, summary, LocalDateTime.now());
    }

    private void publish(UUID assignmentId, UUID tenantId, UUID projectId, UUID userId,
                         UUID actorId, String eventType, String summary, LocalDateTime occurredAt) {
        events.publishEvent(new ProjectAssignmentChanged(
                UUID.randomUUID(), tenantId, projectId, assignmentId, userId,
                actorId, eventType, summary, occurredAt));
    }
}
