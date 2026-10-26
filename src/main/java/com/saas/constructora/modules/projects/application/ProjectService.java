package com.saas.constructora.modules.projects.application;

import com.saas.constructora.modules.identity.application.TenantQueryService;
import com.saas.constructora.modules.identity.domain.Role;
import com.saas.constructora.modules.projects.infrastructure.Project;
import com.saas.constructora.modules.projects.infrastructure.ProjectAssignmentRepository;
import com.saas.constructora.modules.projects.infrastructure.ProjectRepository;
import com.saas.constructora.modules.projects.interfaces.dto.CreateProjectRequest;
import com.saas.constructora.modules.projects.interfaces.dto.ProjectResponse;
import com.saas.constructora.modules.projects.interfaces.dto.UpdateProjectRequest;
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
    public ProjectResponse create(UUID tenantId, Role role, CreateProjectRequest req) {
        if (!role.canCreateProject()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere rol de administrador para crear proyectos");
        }
        tenantQueryService.ensureExists(tenantId);
        return ProjectResponse.from(repository.save(new Project(tenantId, req.toDetails())));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID tenantId, UUID userId, Role role) {
        if (role.canViewAllProjects()) {
            return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                    .map(ProjectResponse::from).toList();
        }
        List<UUID> ids = assignmentRepository.findProjectIdsByTenantIdAndUserId(tenantId, userId);
        if (ids.isEmpty()) return List.of();
        return repository.findByTenantIdAndIdInOrderByCreatedAtDesc(tenantId, ids).stream()
                .map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public void ensureBelongsToTenant(UUID projectId, UUID tenantId) {
        requireProjectOfTenant(tenantId, projectId);
    }

    @Transactional(readOnly = true)
    public void ensureUserCanAccessProject(UUID projectId, UUID tenantId, UUID userId, Role role) {
        requireAccessibleProject(projectId, tenantId, userId, role);
    }

    @Transactional
    public ProjectResponse update(UUID tenantId, UUID projectId, UUID userId, Role role,
                                  UpdateProjectRequest req) {
        Project project = requireAccessibleProject(projectId, tenantId, userId, role);
        project.updateDetails(req.toDetails());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID userId, Role role) {
        if (!role.canDeleteProject()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Se requiere rol de administrador para eliminar proyectos");
        }
        repository.delete(requireProjectOfTenant(tenantId, projectId));
    }

    private Project requireAccessibleProject(UUID projectId, UUID tenantId, UUID userId, Role role) {
        Project project = requireProjectOfTenant(tenantId, projectId);
        if (!role.canViewAllProjects() && !assignmentRepository
                .existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return project;
    }

    private Project requireProjectOfTenant(UUID tenantId, UUID projectId) {
        return repository.findByIdAndTenantId(projectId, tenantId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }
}
