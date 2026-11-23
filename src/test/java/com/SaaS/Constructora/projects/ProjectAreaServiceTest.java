package com.SaaS.Constructora.projects;
import com.SaaS.Constructora.projects.application.ProjectAreaService;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectAreaRequest;

import com.SaaS.Constructora.identity.application.TaskCreationPermissionService;
import com.SaaS.Constructora.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectAreaServiceTest {

    private final ProjectAreaRepository repository = mock(ProjectAreaRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TaskCreationPermissionService permissionService =
            mock(TaskCreationPermissionService.class);
    private final ProjectAreaService service =
            new ProjectAreaService(repository, projectService, permissionService);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void creaElAreaEnElProyecto() {
        when(repository.save(any(ProjectArea.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(tenantId, projectId, userId, Role.WORKER,
                new CreateProjectAreaRequest("Cimentación"));

        assertEquals("Cimentación", response.name());
        assertEquals(projectId, response.projectId());
        verify(permissionService).requireCanCreateTasks(tenantId, userId);
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.WORKER);
    }

    @Test
    void creaElAreaConElTipoIndicado() {
        when(repository.save(any(ProjectArea.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(tenantId, projectId, userId, Role.WORKER,
                new CreateProjectAreaRequest("Obra / Ingeniería", AreaTipo.OBRA_INGENIERIA));

        assertEquals(AreaTipo.OBRA_INGENIERIA, response.tipo());
        assertEquals("Obra / Ingeniería", response.name());
    }

    @Test
    void eliminaUnAreaDelProyecto() {
        ProjectArea area = new ProjectArea(tenantId, projectId, "Cimentación");
        when(repository.findByIdAndTenantIdAndProjectId(area.getId(), tenantId, projectId))
                .thenReturn(Optional.of(area));

        service.delete(tenantId, projectId, area.getId(), userId, Role.ADMIN);

        verify(repository).delete(area);
    }

    @Test
    void areaDeOtroProyectoNoSePuedeEliminar() {
        UUID areaId = UUID.randomUUID();
        when(repository.findByIdAndTenantIdAndProjectId(areaId, tenantId, projectId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, areaId, userId, Role.ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(ProjectArea.class));
    }

    @Test
    void sinPermisoNoPuedeGestionarAreas() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "sin permiso"))
                .when(permissionService).requireCanCreateTasks(tenantId, userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateProjectAreaRequest("Área")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(projectService, repository);
    }
}
