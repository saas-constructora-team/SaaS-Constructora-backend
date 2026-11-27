package com.SaaS.Constructora.projects.application;

import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.infrastructure.Project;
import com.SaaS.Constructora.projects.infrastructure.ProjectRepository;
import com.SaaS.Constructora.projects.interfaces.ProjectProgressResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectProgressRequest;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Etapa en curso del proyecto. A diferencia de la selección del navegador,
 * este dato se persiste y se valida con el mismo aislamiento que el resto de
 * operaciones del módulo: ADMIN ve todos los proyectos del tenant y MEMBER
 * solo los que tiene asignados.
 */
@Service
public class ProjectProgressService {

    private final ProjectRepository repository;
    private final ProjectService projects;
    private final ApplicationEventPublisher events;

    public ProjectProgressService(ProjectRepository repository, ProjectService projects,
                                  ApplicationEventPublisher events) {
        this.repository = repository;
        this.projects = projects;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public ProjectProgressResponse get(UUID tenantId, UUID projectId,
                                       UUID userId, Role role) {
        Project project = requireProject(tenantId, projectId, userId, role);
        return ProjectProgressResponse.from(project);
    }

    @Transactional
    public ProjectProgressResponse select(UUID tenantId, UUID projectId,
                                          UUID userId, Role role,
                                          UpdateProjectProgressRequest request) {
        Project project = requireProject(tenantId, projectId, userId, role);
        projects.ensureStageBelongsToProjectAndTenant(request.stageId(), projectId, tenantId);
        projects.ensureStageAllowsEntries(request.stageId());

        LocalDateTime now = LocalDateTime.now();
        project.selectCurrentStage(request.stageId(), userId, now);
        // @Version se incrementa en el flush: hay que forzarlo para devolver
        // la versión real y detectar cambios concurrentes con un 409.
        Project saved = repository.saveAndFlush(project);
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.PROGRESS, projectId, userId));
        return ProjectProgressResponse.from(saved);
    }

    private Project requireProject(UUID tenantId, UUID projectId, UUID userId, Role role) {
        Project project = repository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        projects.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        return project;
    }
}
