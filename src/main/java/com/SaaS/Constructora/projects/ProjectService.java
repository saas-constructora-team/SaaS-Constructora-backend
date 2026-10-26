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
    private final ProjectStageRepository stageRepository;

    public ProjectService(ProjectRepository repository, TenantQueryService tenantQueryService,
                          ProjectAssignmentRepository assignmentRepository,
                          ProjectStageRepository stageRepository) {
        this.repository = repository;
        this.tenantQueryService = tenantQueryService;
        this.assignmentRepository = assignmentRepository;
        this.stageRepository = stageRepository;
    }

    @Transactional
    public ProjectResponse create(UUID tenantId, Role role, CreateProjectRequest req) {
        requireCanCreate(role);
        // Validación contextual: la empresa debe existir antes de asociarle proyectos.
        tenantQueryService.ensureExists(tenantId);
        Project project = new Project(tenantId, req.toDetails());
        Project saved = repository.save(project);
        stageRepository.saveAll(ProjectStage.initialStages(tenantId, saved.getId()));
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

    /**
     * Valida que el proyecto exista en el tenant y que el usuario pueda
     * acceder a él. ADMIN accede a todos; MEMBER solo a los asignados.
     * Un proyecto no asignado se reporta como inexistente (404) para no
     * revelar su existencia.
     */
    @Transactional(readOnly = true)
    public void ensureUserCanAccessProject(UUID projectId, UUID tenantId,
                                           UUID userId, Role role) {
        requireAccessibleProject(projectId, tenantId, userId, role);
    }

    /**
     * ADMIN edita cualquier proyecto de su empresa; un MEMBER solo los que
     * tiene asignados. El proyecto se resuelve una sola vez para aplicar el
     * aislamiento de tenant y la autorización sobre la misma entidad.
     */
    @Transactional
    public ProjectResponse update(UUID tenantId, UUID projectId, UUID userId, Role role,
                                  UpdateProjectRequest req) {
        Project project = requireAccessibleProject(projectId, tenantId, userId, role);
        project.updateDetails(req.toDetails());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID userId, Role role) {
        requireCanDelete(role);
        repository.delete(requireProjectOfTenant(tenantId, projectId));
    }

    private void requireCanCreate(Role role) {
        if (!role.canCreateProject()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador para crear proyectos");
        }
    }

    private void requireCanDelete(Role role) {
        if (!role.canDeleteProject()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador para eliminar proyectos");
        }
    }

    /**
     * Valida tenant y acceso en una sola resolución del proyecto. Un proyecto
     * no asignado se reporta como inexistente (404) para no revelar su
     * existencia a un MEMBER.
     */
    private Project requireAccessibleProject(UUID projectId, UUID tenantId,
                                             UUID userId, Role role) {
        Project project = requireProjectOfTenant(tenantId, projectId);
        if (!role.canViewAllProjects()
                && !assignmentRepository.existsByTenantIdAndProjectIdAndUserId(
                        tenantId, projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return project;
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
