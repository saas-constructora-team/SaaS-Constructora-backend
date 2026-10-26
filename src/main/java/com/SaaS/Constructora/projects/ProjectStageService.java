package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de las etapas de un proyecto. El acceso se delega en
 * {@link ProjectService}: ADMIN ve todos los proyectos y MEMBER solo los
 * proyectos que tiene asignados.
 */
@Service
public class ProjectStageService {

    private final ProjectStageRepository repository;
    private final ProjectService projectService;

    public ProjectStageService(ProjectStageRepository repository, ProjectService projectService) {
        this.repository = repository;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public List<ProjectStageResponse> list(UUID tenantId, UUID projectId,
                                           UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        return repository.findByTenantIdAndProjectIdOrderByStageOrderAsc(tenantId, projectId)
                .stream()
                .map(ProjectStageResponse::from)
                .toList();
    }

    @Transactional
    public ProjectStageResponse create(UUID tenantId, UUID projectId, UUID userId, Role role,
                                       CreateProjectStageRequest request) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);

        int nextOrder = repository.findMaxStageOrder(tenantId, projectId) + 1;
        ProjectStage saved = repository.save(
                new ProjectStage(tenantId, projectId, request.name(), nextOrder));
        return ProjectStageResponse.from(saved);
    }

    @Transactional
    public ProjectStageResponse update(UUID tenantId, UUID projectId, UUID stageId,
                                       UUID userId, Role role,
                                       UpdateProjectStageRequest request) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        ProjectStage stage = requireStage(tenantId, projectId, stageId);
        stage.rename(request.name());
        return ProjectStageResponse.from(stage);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID stageId,
                       UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        repository.delete(requireStage(tenantId, projectId, stageId));
    }

    private ProjectStage requireStage(UUID tenantId, UUID projectId, UUID stageId) {
        return repository.findByIdAndTenantIdAndProjectId(stageId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Etapa no encontrada"));
    }
}
