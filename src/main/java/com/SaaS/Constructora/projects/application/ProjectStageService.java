package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;
import com.SaaS.Constructora.projects.infrastructure.ProjectStageRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectStageRequest;
import com.SaaS.Constructora.projects.interfaces.ProjectStageResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectStageRequest;

import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.events.ProjectStageClosed;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher events;

    public ProjectStageService(ProjectStageRepository repository, ProjectService projectService,
                               ApplicationEventPublisher events) {
        this.repository = repository;
        this.projectService = projectService;
        this.events = events;
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
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.STAGE, saved.getId(), userId));
        return ProjectStageResponse.from(saved);
    }

    @Transactional
    public ProjectStageResponse update(UUID tenantId, UUID projectId, UUID stageId,
                                       UUID userId, Role role,
                                       UpdateProjectStageRequest request) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        ProjectStage stage = requireStage(tenantId, projectId, stageId);
        try {
            stage.rename(request.name());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.STAGE, stageId, userId));
        return ProjectStageResponse.from(stage);
    }

    /** Cierra la etapa y publica los eventos que consumen auditoría y realtime. */
    @Transactional
    public ProjectStageResponse close(UUID tenantId, UUID projectId, UUID stageId,
                                      UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        ProjectStage stage = requireStage(tenantId, projectId, stageId);
        try {
            stage.close();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        events.publishEvent(new ProjectStageClosed(
                UUID.randomUUID(), tenantId, projectId, stage.getId(), userId,
                stage.getName(), stage.getStageOrder(), stage.getClosedAt()));
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.STAGE, stage.getId(), userId));
        return ProjectStageResponse.from(stage);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID stageId,
                       UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        ProjectStage stage = requireStage(tenantId, projectId, stageId);
        repository.delete(stage);
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.STAGE, stage.getId(), userId));
    }

    private ProjectStage requireStage(UUID tenantId, UUID projectId, UUID stageId) {
        return repository.findByIdAndTenantIdAndProjectId(stageId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Etapa no encontrada"));
    }
}
