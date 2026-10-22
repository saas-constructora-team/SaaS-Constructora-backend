package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.TenantQueryService;
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

    public ProjectService(ProjectRepository repository, TenantQueryService tenantQueryService) {
        this.repository = repository;
        this.tenantQueryService = tenantQueryService;
    }

    @Transactional
    public ProjectResponse create(UUID tenantId, CreateProjectRequest req) {
        // Validación contextual: la empresa debe existir antes de asociarle proyectos.
        tenantQueryService.ensureExists(tenantId);
        Project project = new Project(tenantId, req.name(), req.clientName(), req.location());
        Project saved = repository.save(project);
        return ProjectResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse update(UUID tenantId, UUID projectId, UpdateProjectRequest req) {
        Project project = requireProjectOfTenant(tenantId, projectId);
        project.updateDetails(req.name(), req.clientName(), req.location());
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
