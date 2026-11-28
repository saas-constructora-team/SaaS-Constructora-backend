package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;
import com.SaaS.Constructora.projects.infrastructure.ProjectStageRepository;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import com.SaaS.Constructora.projects.infrastructure.Task;
import com.SaaS.Constructora.projects.infrastructure.TaskAreaDetails;
import com.SaaS.Constructora.projects.infrastructure.TaskRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.AreaDetailsRequest;
import com.SaaS.Constructora.projects.interfaces.CreateTaskRequest;
import com.SaaS.Constructora.projects.interfaces.TaskBoardResponse;
import com.SaaS.Constructora.projects.interfaces.TaskResponse;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso de las tareas de un proyecto. El permiso de creación se valida
 * contra la membresía del tenant y el acceso al proyecto se delega en
 * {@link ProjectService}: ADMIN ve todos los proyectos y el resto solo los
 * proyectos que tiene asignados.
 */
@Service
public class TaskService {

    private static final int MAX_BOARD_LIMIT = 1000;

    private final TaskRepository repository;
    private final ProjectService projectService;
    private final TaskCreationPermissionService permissionService;
    private final TaskStatusRepository statusRepository;
    private final ProjectAreaRepository areaRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectStageRepository stageRepository;

    public TaskService(TaskRepository repository, ProjectService projectService,
                       TaskCreationPermissionService permissionService,
                       TaskStatusRepository statusRepository,
                       ProjectAreaRepository areaRepository,
                       ProjectAssignmentRepository assignmentRepository,
                       ProjectStageRepository stageRepository) {
        this.repository = repository;
        this.projectService = projectService;
        this.permissionService = permissionService;
        this.statusRepository = statusRepository;
        this.areaRepository = areaRepository;
        this.assignmentRepository = assignmentRepository;
        this.stageRepository = stageRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(UUID tenantId, UUID projectId, UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);

        List<Task> tasks = repository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId);
        Map<UUID, TaskStatus> statuses = statusRepository
                .findByTenantIdAndProjectIdOrderByStatusOrderAsc(tenantId, projectId)
                .stream()
                .collect(Collectors.toMap(TaskStatus::getId, Function.identity()));
        Map<UUID, ProjectArea> areas = areaRepository
                .findByTenantIdAndProjectIdOrderByNameAsc(tenantId, projectId)
                .stream()
                .collect(Collectors.toMap(ProjectArea::getId, Function.identity()));
        Map<UUID, ProjectAssignment> assignments = assignmentRepository
                .findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId)
                .stream()
                .collect(Collectors.toMap(ProjectAssignment::getId, Function.identity()));

        return tasks.stream()
                .map(task -> toResponse(task, statuses, areas, assignments))
                .toList();
    }

    /**
     * Tablero global en una sola lectura. ADMIN ve todos los proyectos del
     * tenant y el resto solo los asignados; todas las consultas llevan tenantId.
     * El límite evita respuestas ilimitadas y se informa cuando se recorta.
     */
    @Transactional(readOnly = true)
    public TaskBoardResponse getBoard(UUID tenantId, UUID userId, Role role,
                                      UUID projectId, int limit) {
        List<UUID> projectIds;
        if (projectId == null) {
            projectIds = projectService.listVisibleProjectIds(tenantId, userId, role);
        } else {
            projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
            projectIds = List.of(projectId);
        }
        if (projectIds.isEmpty()) {
            return new TaskBoardResponse(List.of(), List.of(), false);
        }

        int safeLimit = Math.min(Math.max(limit, 1), MAX_BOARD_LIMIT);
        Pageable page = PageRequest.of(0, safeLimit + 1,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Map<UUID, TaskStatus> statuses = statusRepository
                .findByTenantIdAndProjectIdInOrderByStatusOrderAsc(tenantId, projectIds)
                .stream()
                .collect(Collectors.toMap(TaskStatus::getId, Function.identity()));
        Map<UUID, ProjectArea> areas = areaRepository
                .findByTenantIdAndProjectIdInOrderByNameAsc(tenantId, projectIds)
                .stream()
                .collect(Collectors.toMap(ProjectArea::getId, Function.identity()));
        Map<UUID, ProjectAssignment> assignments = assignmentRepository
                .findActiveByTenantIdAndProjectIdIn(tenantId, projectIds)
                .stream()
                .collect(Collectors.toMap(ProjectAssignment::getId, Function.identity()));

        List<TaskBoardResponse.Stage> stages = stageRepository
                .findByTenantIdAndProjectIdInOrderByStageOrderAsc(tenantId, projectIds)
                .stream()
                .map(stage -> new TaskBoardResponse.Stage(stage.getId(), stage.getProjectId(),
                        stage.getName(), stage.getStageOrder(), stage.getColor(), stage.isClosed()))
                .toList();

        List<Task> found = repository.findByTenantIdAndProjectIdIn(tenantId, projectIds, page);
        boolean truncated = found.size() > safeLimit;
        List<TaskResponse> tasks = found.stream()
                .limit(safeLimit)
                .map(task -> toResponse(task, statuses, areas, assignments))
                .toList();
        return new TaskBoardResponse(stages, tasks, truncated);
    }

    /**
     * Mueve una tarea a otra etapa de su mismo proyecto, o la deja sin etapa
     * cuando `stageId` es nulo. No cambia el proyecto: la etapa destino se
     * valida contra el proyecto y el tenant de la tarea.
     */
    @Transactional
    public TaskResponse moveToStage(UUID tenantId, UUID taskId, UUID userId, Role role,
                                    UUID stageId) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        Task task = repository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        projectService.ensureUserCanAccessProject(task.getProjectId(), tenantId, userId, role);

        if (stageId != null) {
            projectService.ensureStageBelongsToProjectAndTenant(
                    stageId, task.getProjectId(), tenantId);
            projectService.ensureStageAllowsEntries(stageId);
        }

        task.moveToStage(stageId);
        return toResponse(repository.save(task), tenantId);
    }

    @Transactional
    public TaskResponse create(UUID tenantId, UUID projectId, UUID userId, Role role,
                               CreateTaskRequest request) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);

        UUID stageId = request.stageId();
        if (stageId != null) {
            projectService.ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
            projectService.ensureStageAllowsEntries(stageId);
        }

        TaskStatus status = request.statusId() == null
                ? statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Estado por defecto no encontrado"))
                : requireStatus(tenantId, projectId, request.statusId());
        ProjectArea area = request.areaId() == null ? null : requireArea(tenantId, projectId, request.areaId());
        TaskAreaDetailsValidator.validate(request.areaDetails(), area == null ? null : area.getTipo());
        ProjectAssignment assignee = request.assigneeUserId() == null ? null
                : requireAssignment(tenantId, projectId, request.assigneeUserId());
        ProjectAssignment validator = request.validatorUserId() == null ? null
                : requireAssignment(tenantId, projectId, request.validatorUserId());

        Task saved = repository.save(new Task(tenantId, projectId, userId, request.title(),
                request.description(), request.fechaFinPrevista(), toDetails(request.areaDetails()),
                status.getId(), area == null ? null : area.getId(),
                assignee == null ? null : assignee.getId(),
                validator == null ? null : validator.getId(), stageId));
        return TaskResponse.from(saved, status, area, assignee, validator);
    }

    /**
     * Edita los campos de una tarea existente sin cambiar su proyecto. La etapa
     * actual se conserva aunque esté cerrada; solo se valida cuando cambia.
     */
    @Transactional
    public TaskResponse update(UUID tenantId, UUID taskId, UUID userId, Role role,
                               CreateTaskRequest request) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        Task task = repository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        UUID projectId = task.getProjectId();
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);

        UUID stageId = request.stageId();
        if (stageId != null && !stageId.equals(task.getStageId())) {
            projectService.ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
            projectService.ensureStageAllowsEntries(stageId);
        }

        TaskStatus status = request.statusId() == null
                ? statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Estado por defecto no encontrado"))
                : requireStatus(tenantId, projectId, request.statusId());
        ProjectArea area = request.areaId() == null ? null
                : requireArea(tenantId, projectId, request.areaId());
        TaskAreaDetailsValidator.validate(request.areaDetails(), area == null ? null : area.getTipo());
        ProjectAssignment assignee = request.assigneeUserId() == null ? null
                : requireAssignment(tenantId, projectId, request.assigneeUserId());
        ProjectAssignment validator = request.validatorUserId() == null ? null
                : requireAssignment(tenantId, projectId, request.validatorUserId());

        task.update(request.title(), request.description(), request.fechaFinPrevista(),
                toDetails(request.areaDetails()), status.getId(), area == null ? null : area.getId(),
                assignee == null ? null : assignee.getId(),
                validator == null ? null : validator.getId(), stageId);
        return toResponse(repository.save(task), tenantId);
    }

    private TaskAreaDetails toDetails(AreaDetailsRequest details) {
        if (details == null) {
            return new TaskAreaDetails();
        }
        return new TaskAreaDetails(details.frenteObra(), details.capituloPresupuestal(),
                details.unidadMedida(), details.cantidadProgramada(), details.contratistaId(),
                Boolean.TRUE.equals(details.requierePermisoSst()), details.permisosSst(),
                details.codigoRequisicion(), details.fechaRequeridaEnObra(),
                details.categoriaInsumo(), details.presupuestoEstimado(), details.proveedorSugerido(),
                details.disciplina(), details.codigoPlano(), details.versionPlano(), details.entidad(),
                details.numeroRadicado(), details.tipoDocumento(), details.numeroDocumento(),
                details.monto());
    }

    private TaskResponse toResponse(Task task, Map<UUID, TaskStatus> statuses,
                                    Map<UUID, ProjectArea> areas,
                                    Map<UUID, ProjectAssignment> assignments) {
        TaskStatus status = statuses.get(task.getStatusId());
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estado no encontrado");
        }
        return TaskResponse.from(task, status, areas.get(task.getAreaId()),
                assignments.get(task.getAssigneeAssignmentId()),
                assignments.get(task.getValidatorAssignmentId()));
    }

    /** Compone la respuesta de una sola tarea sin consultar el tablero completo. */
    private TaskResponse toResponse(Task task, UUID tenantId) {
        UUID projectId = task.getProjectId();
        TaskStatus status = requireStatus(tenantId, projectId, task.getStatusId());
        ProjectArea area = task.getAreaId() == null ? null
                : requireArea(tenantId, projectId, task.getAreaId());
        ProjectAssignment assignee = task.getAssigneeAssignmentId() == null ? null
                : requireAssignmentById(tenantId, projectId, task.getAssigneeAssignmentId());
        ProjectAssignment validator = task.getValidatorAssignmentId() == null ? null
                : requireAssignmentById(tenantId, projectId, task.getValidatorAssignmentId());
        return TaskResponse.from(task, status, area, assignee, validator);
    }

    private TaskStatus requireStatus(UUID tenantId, UUID projectId, UUID statusId) {
        return statusRepository.findByIdAndTenantIdAndProjectId(statusId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estado no encontrado"));
    }

    private ProjectArea requireArea(UUID tenantId, UUID projectId, UUID areaId) {
        return areaRepository.findByIdAndTenantIdAndProjectId(areaId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Área no encontrada"));
    }

    private ProjectAssignment requireAssignment(UUID tenantId, UUID projectId, UUID userId) {
        return assignmentRepository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no asignado al proyecto"));
    }

    private ProjectAssignment requireAssignmentById(UUID tenantId, UUID projectId, UUID assignmentId) {
        return assignmentRepository.findActiveByIdAndTenantIdAndProjectId(
                        assignmentId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Asignación no encontrada"));
    }
}
