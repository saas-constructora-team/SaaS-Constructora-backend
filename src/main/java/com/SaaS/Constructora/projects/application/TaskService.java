package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import com.SaaS.Constructora.projects.infrastructure.Task;
import com.SaaS.Constructora.projects.infrastructure.TaskAreaDetails;
import com.SaaS.Constructora.projects.infrastructure.TaskRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.AreaDetailsRequest;
import com.SaaS.Constructora.projects.interfaces.CreateTaskRequest;
import com.SaaS.Constructora.projects.interfaces.TaskResponse;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
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

    private final TaskRepository repository;
    private final ProjectService projectService;
    private final TaskCreationPermissionService permissionService;
    private final TaskStatusRepository statusRepository;
    private final ProjectAreaRepository areaRepository;
    private final ProjectAssignmentRepository assignmentRepository;

    public TaskService(TaskRepository repository, ProjectService projectService,
                       TaskCreationPermissionService permissionService,
                       TaskStatusRepository statusRepository,
                       ProjectAreaRepository areaRepository,
                       ProjectAssignmentRepository assignmentRepository) {
        this.repository = repository;
        this.projectService = projectService;
        this.permissionService = permissionService;
        this.statusRepository = statusRepository;
        this.areaRepository = areaRepository;
        this.assignmentRepository = assignmentRepository;
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

    @Transactional
    public TaskResponse create(UUID tenantId, UUID projectId, UUID userId, Role role,
                               CreateTaskRequest request) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);

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
                validator == null ? null : validator.getId()));
        return TaskResponse.from(saved, status, area, assignee, validator);
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
}
