package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.domain.ProjectType;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.infrastructure.Project;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;
import com.SaaS.Constructora.projects.infrastructure.ProjectStageRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectRequest;
import com.SaaS.Constructora.projects.interfaces.ProjectResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectRequest;

import com.SaaS.Constructora.identity.application.TenantQueryService;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectService implements ProjectTenantPort {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProjectRepository repository;
    private final TenantQueryService tenantQueryService;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectStageRepository stageRepository;
    private final TaskStatusRepository statusRepository;
    private final ProjectAreaRepository areaRepository;
    private final ApplicationEventPublisher events;

    public ProjectService(ProjectRepository repository, TenantQueryService tenantQueryService,
                          ProjectAssignmentRepository assignmentRepository,
                          ProjectStageRepository stageRepository,
                          TaskStatusRepository statusRepository,
                          ProjectAreaRepository areaRepository,
                          ApplicationEventPublisher events) {
        this.repository = repository;
        this.tenantQueryService = tenantQueryService;
        this.assignmentRepository = assignmentRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.areaRepository = areaRepository;
        this.events = events;
    }

    @Transactional
    public ProjectResponse create(UUID tenantId, UUID userId, Role role, CreateProjectRequest req) {
        requireCanCreate(role);
        // Validación contextual: la empresa debe existir antes de asociarle proyectos.
        tenantQueryService.ensureExists(tenantId);
        Project project = new Project(tenantId, req.toDetails());
        Project saved = repository.save(project);
        stageRepository.saveAll(ProjectStage.initialStages(tenantId, saved.getId()));
        statusRepository.saveAll(TaskStatus.initialStatuses(tenantId, saved.getId()));
        areaRepository.saveAll(ProjectArea.initialAreas(tenantId, saved.getId()));
        events.publishEvent(new ProjectCreated(
                UUID.randomUUID(), tenantId, saved.getId(), userId,
                saved.getName(), saved.getCreatedAt()));
        return ProjectResponse.from(saved);
    }

    /**
     * ADMIN (y futuros roles altos) ve todos los proyectos de la empresa.
     * El resto solo ve los proyectos que tiene asignados. La visibilidad se
     * resuelve en una única consulta con el tenant incluido.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID tenantId, UUID userId, Role role) {
        return repository.findVisibleProjects(tenantId, userId, role.canViewAllProjects())
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /** Búsqueda paginada con filtros opcionales, restringida al tenant y a la visibilidad. */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> search(UUID tenantId, UUID userId, Role role, String query,
                                        ProjectStatus status, ProjectType projectType,
                                        LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String namePattern = (query == null || query.isBlank())
                ? null
                : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        Sort allowedSort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest safePage = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                allowedSort);
        return repository.searchProjects(tenantId, userId, role.canViewAllProjects(),
                        namePattern, status, projectType, from, to, safePage)
                .map(ProjectResponse::from);
    }

    /**
     * API pública del módulo para que otros casos de uso validen que un
     * proyecto pertenece al tenant antes de operar sobre él.
     */
    @Transactional(readOnly = true)
    public void ensureBelongsToTenant(UUID projectId, UUID tenantId) {
        requireProjectOfTenant(tenantId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAssignedToProject(UUID projectId, UUID tenantId, UUID userId) {
        return assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId);
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
                && !assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
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
