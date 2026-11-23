package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectAreaRequest;
import com.SaaS.Constructora.projects.interfaces.ProjectAreaResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectAreaRequest;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Áreas de trabajo de un proyecto. */
@Service
public class ProjectAreaService {

    private final ProjectAreaRepository repository;
    private final ProjectService projectService;
    private final TaskCreationPermissionService permissionService;

    public ProjectAreaService(ProjectAreaRepository repository, ProjectService projectService,
                              TaskCreationPermissionService permissionService) {
        this.repository = repository;
        this.projectService = projectService;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<ProjectAreaResponse> list(UUID tenantId, UUID projectId,
                                          UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        return repository.findByTenantIdAndProjectIdOrderByNameAsc(tenantId, projectId)
                .stream()
                .map(ProjectAreaResponse::from)
                .toList();
    }

    @Transactional
    public ProjectAreaResponse create(UUID tenantId, UUID projectId, UUID userId, Role role,
                                      CreateProjectAreaRequest request) {
        requireCanManage(tenantId, projectId, userId, role);
        return ProjectAreaResponse.from(
                repository.save(new ProjectArea(tenantId, projectId, request.name(), request.tipo())));
    }

    @Transactional
    public ProjectAreaResponse update(UUID tenantId, UUID projectId, UUID areaId,
                                      UUID userId, Role role, UpdateProjectAreaRequest request) {
        requireCanManage(tenantId, projectId, userId, role);
        ProjectArea area = requireArea(tenantId, projectId, areaId);
        area.rename(request.name());
        return ProjectAreaResponse.from(repository.save(area));
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID areaId,
                       UUID userId, Role role) {
        requireCanManage(tenantId, projectId, userId, role);
        repository.delete(requireArea(tenantId, projectId, areaId));
    }

    private void requireCanManage(UUID tenantId, UUID projectId, UUID userId, Role role) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
    }

    private ProjectArea requireArea(UUID tenantId, UUID projectId, UUID areaId) {
        return repository.findByIdAndTenantIdAndProjectId(areaId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Área no encontrada"));
    }
}
