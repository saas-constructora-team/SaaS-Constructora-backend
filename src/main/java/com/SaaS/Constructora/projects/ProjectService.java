package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.TenantQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
}
