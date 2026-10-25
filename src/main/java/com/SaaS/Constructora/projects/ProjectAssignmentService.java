package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.application.UserSummary;
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

    @Transactional
    public ProjectAssignmentResponse assign(UUID tenantId, UUID projectId, UUID userId) {
        projectService.ensureBelongsToTenant(projectId, tenantId);
        UserSummary user = userQueryService.ensureBelongsToTenant(userId, tenantId);

        if (repository.existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "El usuario ya está asignado a este proyecto");
        }

        ProjectAssignment saved = repository.save(
                new ProjectAssignment(tenantId, projectId, userId));
        return ProjectAssignmentResponse.from(saved, user);
    }

    @Transactional(readOnly = true)
    public List<ProjectAssignmentResponse> list(UUID tenantId, UUID projectId) {
        projectService.ensureBelongsToTenant(projectId, tenantId);

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
     * Desasigna un usuario del proyecto. La operación es idempotente: si la
     * asignación ya no existe, igualmente responde 204.
     */
    @Transactional
    public void unassign(UUID tenantId, UUID projectId, UUID userId) {
        projectService.ensureBelongsToTenant(projectId, tenantId);
        userQueryService.ensureBelongsToTenant(userId, tenantId);
        repository.deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }
}
