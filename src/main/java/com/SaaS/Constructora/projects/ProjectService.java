package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.TenantQueryService;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository repository;
    private final TenantQueryService tenantQueryService;
    private final ProjectAssignmentRepository assignmentRepository;

    public ProjectService(ProjectRepository repository, TenantQueryService tenantQueryService,
                          ProjectAssignmentRepository assignmentRepository) {
        this.repository = repository;
        this.tenantQueryService = tenantQueryService;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public ProjectResponse create(UUID tenantId, CreateProjectRequest req) {
        // Validación contextual: la empresa debe existir antes de asociarle proyectos.
        tenantQueryService.ensureExists(tenantId);
        Project project = new Project(tenantId, req.toDetails());
        Project saved = repository.save(project);
        return ProjectResponse.from(saved);
    }

    /**
     * ADMIN (y futuros roles altos) ve todos los proyectos de la empresa.
     * El resto solo ve los proyectos que tiene asignados.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID tenantId, UUID userId, Role role) {
        if (role.canViewAllProjects()) {
            return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                    .stream()
                    .map(ProjectResponse::from)
                    .toList();
        }

        List<UUID> assignedProjectIds =
                assignmentRepository.findProjectIdsByTenantIdAndUserId(tenantId, userId);
        if (assignedProjectIds.isEmpty()) {
            return List.of();
        }

        return repository.findByTenantIdAndIdInOrderByCreatedAtDesc(tenantId, assignedProjectIds)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /**
     * API pública del módulo para que otros casos de uso validen que un
     * proyecto pertenece al tenant antes de operar sobre él.
     */
    @Transactional(readOnly = true)
    public void ensureBelongsToTenant(UUID projectId, UUID tenantId) {
        requireProjectOfTenant(tenantId, projectId);
    }

    @Transactional
    public ProjectResponse update(UUID tenantId, UUID projectId, UpdateProjectRequest req) {
        Project project = requireProjectOfTenant(tenantId, projectId);
        project.updateDetails(req.toDetails());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId) {
        repository.delete(requireProjectOfTenant(tenantId, projectId));
    }

    /**
     * Un proyecto de otra empresa se reporta como inexistente (404) para no
     * revelar que existe fuera del tenant autenticado.
     */
    private Project requireProjectOfTenant(UUID tenantId, UUID projectId) {
        return repository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }
}
