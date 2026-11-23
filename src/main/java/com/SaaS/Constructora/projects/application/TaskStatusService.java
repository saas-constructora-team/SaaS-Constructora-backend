package com.SaaS.Constructora.projects.application;
import com.SaaS.Constructora.projects.infrastructure.TaskRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.CreateTaskStatusRequest;
import com.SaaS.Constructora.projects.interfaces.TaskStatusResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateTaskStatusRequest;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Catálogo de estados de tareas de un proyecto. */
@Service
public class TaskStatusService {

    private final TaskStatusRepository repository;
    private final ProjectService projectService;
    private final TaskRepository taskRepository;
    private final TaskCreationPermissionService permissionService;

    public TaskStatusService(TaskStatusRepository repository, ProjectService projectService,
                             TaskRepository taskRepository,
                             TaskCreationPermissionService permissionService) {
        this.repository = repository;
        this.projectService = projectService;
        this.taskRepository = taskRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<TaskStatusResponse> list(UUID tenantId, UUID projectId,
                                         UUID userId, Role role) {
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
        return repository.findByTenantIdAndProjectIdOrderByStatusOrderAsc(tenantId, projectId)
                .stream()
                .map(TaskStatusResponse::from)
                .toList();
    }

    @Transactional
    public TaskStatusResponse create(UUID tenantId, UUID projectId, UUID userId, Role role,
                                     CreateTaskStatusRequest request) {
        requireCanManage(tenantId, projectId, userId, role);

        boolean first = repository.countByTenantIdAndProjectId(tenantId, projectId) == 0;
        boolean makeDefault = first || Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            repository.clearDefault(tenantId, projectId);
        }

        int nextOrder = repository.findMaxStatusOrder(tenantId, projectId) + 1;
        TaskStatus saved = repository.save(new TaskStatus(tenantId, projectId, request.name(),
                request.color(), nextOrder, makeDefault));
        return TaskStatusResponse.from(saved);
    }

    @Transactional
    public TaskStatusResponse update(UUID tenantId, UUID projectId, UUID statusId,
                                     UUID userId, Role role, UpdateTaskStatusRequest request) {
        requireCanManage(tenantId, projectId, userId, role);
        TaskStatus status = requireStatus(tenantId, projectId, statusId);

        if (Boolean.TRUE.equals(request.isDefault())) {
            repository.clearDefault(tenantId, projectId);
        }
        status.update(request.name(), request.color(), request.isDefault());
        return TaskStatusResponse.from(repository.save(status));
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID statusId,
                       UUID userId, Role role) {
        requireCanManage(tenantId, projectId, userId, role);
        TaskStatus status = requireStatus(tenantId, projectId, statusId);

        if (status.isSystemStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un estado predeterminado del sistema");
        }
        if (status.isDefaultStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el estado por defecto");
        }
        if (taskRepository.existsByTenantIdAndProjectIdAndStatusId(tenantId, projectId, statusId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un estado que tiene tareas asignadas");
        }
        repository.delete(status);
    }

    private void requireCanManage(UUID tenantId, UUID projectId, UUID userId, Role role) {
        permissionService.requireCanCreateTasks(tenantId, userId);
        projectService.ensureUserCanAccessProject(projectId, tenantId, userId, role);
    }

    private TaskStatus requireStatus(UUID tenantId, UUID projectId, UUID statusId) {
        return repository.findByIdAndTenantIdAndProjectId(statusId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Estado no encontrado"));
    }
}
