package com.SaaS.Constructora.projects;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.application.TaskStatusService;
import com.SaaS.Constructora.projects.infrastructure.TaskRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.CreateTaskStatusRequest;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskStatusServiceTest {

    private final TaskStatusRepository repository = mock(TaskStatusRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TaskRepository taskRepository = mock(TaskRepository.class);
    private final TaskCreationPermissionService permissionService =
            mock(TaskCreationPermissionService.class);
    private final TaskStatusService service = new TaskStatusService(
            repository, projectService, taskRepository, permissionService);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private TaskStatus status(boolean isDefault) {
        return new TaskStatus(tenantId, projectId, "Pendiente", "#EF4444", 1, isDefault);
    }

    @Test
    void elPrimerEstadoSiempreQuedaComoDefault() {
        when(repository.countByTenantIdAndProjectId(tenantId, projectId)).thenReturn(0L);
        when(repository.findMaxStatusOrder(tenantId, projectId)).thenReturn(0);
        when(repository.save(any(TaskStatus.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(tenantId, projectId, userId, Role.WORKER,
                new CreateTaskStatusRequest("Pendiente", "#EF4444", false));

        assertTrue(response.isDefault());
        assertEquals(1, response.order());
        verify(repository).clearDefault(tenantId, projectId);
    }

    @Test
    void noEliminaUnEstadoQueTieneTareas() {
        TaskStatus status = status(false);
        when(repository.findByIdAndTenantIdAndProjectId(status.getId(), tenantId, projectId))
                .thenReturn(Optional.of(status));
        when(taskRepository.existsByTenantIdAndProjectIdAndStatusId(
                tenantId, projectId, status.getId())).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, status.getId(), userId, Role.ADMIN));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).delete(any(TaskStatus.class));
    }

    @Test
    void noEliminaElEstadoPorDefecto() {
        TaskStatus status = status(true);
        when(repository.findByIdAndTenantIdAndProjectId(status.getId(), tenantId, projectId))
                .thenReturn(Optional.of(status));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, status.getId(), userId, Role.ADMIN));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(taskRepository);
        verify(repository, never()).delete(any(TaskStatus.class));
    }

    @Test
    void noEliminaElEstadoPredeterminadoDelSistema() {
        TaskStatus status = new TaskStatus(tenantId, projectId, "Terminado", "#22C55E", 3, false, true);
        when(repository.findByIdAndTenantIdAndProjectId(status.getId(), tenantId, projectId))
                .thenReturn(Optional.of(status));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, status.getId(), userId, Role.ADMIN));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(taskRepository);
        verify(repository, never()).delete(any(TaskStatus.class));
    }

    @Test
    void sinPermisoNoPuedeGestionarEstados() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "sin permiso"))
                .when(permissionService).requireCanCreateTasks(tenantId, userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateTaskStatusRequest("Nuevo", "#000000", false)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(projectService, repository);
    }
}
