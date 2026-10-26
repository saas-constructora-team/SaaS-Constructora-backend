package com.saas.constructora.modules.projects.application;

import com.saas.constructora.modules.identity.application.*;
import com.saas.constructora.modules.identity.domain.*;
import com.saas.constructora.modules.projects.application.*;
import com.saas.constructora.modules.projects.domain.*;
import com.saas.constructora.modules.projects.infrastructure.*;
import com.saas.constructora.modules.projects.interfaces.dto.*;

import com.saas.constructora.modules.identity.application.TenantQueryService;
import com.saas.constructora.modules.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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

class ProjectServiceTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final TenantQueryService tenantQueryService = mock(TenantQueryService.class);
    private final ProjectAssignmentRepository assignmentRepository =
            mock(ProjectAssignmentRepository.class);
    
    private final ProjectService service =
            new ProjectService(repository, tenantQueryService, assignmentRepository);

    @SuppressWarnings("unchecked")
    @Test
    void creaElProyectoConElTenantRecibido() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.create(tenantId, Role.ADMIN,
                new CreateProjectRequest("Torre Central", "Inmobiliaria XYZ", "Bogotá"));

        verify(tenantQueryService).ensureExists(tenantId);
        assertEquals(tenantId, response.tenantId());
        assertEquals("Torre Central", response.name());
        assertEquals(ProjectStatus.ACTIVE, response.status());


    }

    @Test
    void noGuardaSiLaEmpresaNoExiste() {
        UUID tenantId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no existe"))
                .when(tenantQueryService).ensureExists(tenantId);

        assertThrows(ResponseStatusException.class, () -> service.create(tenantId, Role.ADMIN,
                new CreateProjectRequest("Torre Central", null, null)));

        verify(repository, never()).save(any(Project.class));
    }

    @Test
    void memberNoPuedeCrearProyectos() {
        UUID tenantId = UUID.randomUUID();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, Role.MEMBER,
                        new CreateProjectRequest("Torre Central", null, null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(tenantQueryService);
        verify(repository, never()).save(any(Project.class));
    }

    @Test
    void adminListaTodosLosProyectosDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());

        List<ProjectResponse> response = service.list(tenantId, userId, Role.ADMIN);

        assertEquals(List.of(), response);
        verify(repository).findByTenantIdOrderByCreatedAtDesc(tenantId);
        verifyNoInteractions(tenantQueryService);
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void memberSoloListaSusProyectosAsignados() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project assigned = new Project(tenantId, "Proyecto asignado", null, null);
        when(assignmentRepository.findProjectIdsByTenantIdAndUserId(tenantId, userId))
                .thenReturn(List.of(projectId));
        when(repository.findByTenantIdAndIdInOrderByCreatedAtDesc(tenantId, List.of(projectId)))
                .thenReturn(List.of(assigned));

        List<ProjectResponse> response = service.list(tenantId, userId, Role.MEMBER);

        assertEquals(1, response.size());
        assertEquals("Proyecto asignado", response.getFirst().name());
        verify(repository, never()).findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Test
    void memberSinAsignacionesNoConsultaProyectos() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(assignmentRepository.findProjectIdsByTenantIdAndUserId(tenantId, userId))
                .thenReturn(List.of());

        List<ProjectResponse> response = service.list(tenantId, userId, Role.MEMBER);

        assertEquals(List.of(), response);
        verify(repository, never()).findByTenantIdOrderByCreatedAtDesc(tenantId);
        verify(repository, never()).findByTenantIdAndIdInOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void adminPuedeAccederAUnProyectoAunqueNoEsteAsignado() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));

        service.ensureUserCanAccessProject(projectId, tenantId, userId, Role.ADMIN);

        verify(assignmentRepository, never())
                .existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberAsignadoPuedeAccederAlProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));
        when(assignmentRepository.existsByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId)).thenReturn(true);

        service.ensureUserCanAccessProject(projectId, tenantId, userId, Role.MEMBER);

        verify(assignmentRepository)
                .existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberNoAsignadoNoPuedeAccederAlProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));
        when(assignmentRepository.existsByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.MEMBER));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void proyectoDeOtraEmpresaNoEsAccesible() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void adminActualizaCualquierProyectoDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = new Project(tenantId, "Nombre viejo", "Cliente viejo", "Ciudad vieja");
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));

        ProjectResponse response = service.update(tenantId, projectId, userId, Role.ADMIN,
                new UpdateProjectRequest("Nombre nuevo", "Cliente nuevo", "Ciudad nueva"));

        assertEquals(project.getId(), response.id());
        assertEquals(tenantId, response.tenantId());
        assertEquals("Nombre nuevo", response.name());
        assertEquals("Cliente nuevo", response.clientName());
        assertEquals("Ciudad nueva", response.location());
        assertEquals(ProjectStatus.ACTIVE, response.status());
        verify(assignmentRepository, never())
                .existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberAsignadoActualizaElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = new Project(tenantId, "Nombre viejo", null, null);
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));
        when(assignmentRepository.existsByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId)).thenReturn(true);

        ProjectResponse response = service.update(tenantId, projectId, userId, Role.MEMBER,
                new UpdateProjectRequest("Nombre nuevo", null, null));

        assertEquals("Nombre nuevo", response.name());
    }

    @Test
    void memberNoAsignadoNoActualizaElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));
        when(assignmentRepository.existsByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId, userId, Role.MEMBER,
                        new UpdateProjectRequest("Nombre nuevo", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void noActualizaUnProyectoDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId, userId, Role.ADMIN,
                        new UpdateProjectRequest("Nombre nuevo", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void adminEliminaElProyectoDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = new Project(tenantId, "Proyecto", null, null);
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));

        service.delete(tenantId, projectId, userId, Role.ADMIN);

        verify(repository).delete(project);
    }

    @Test
    void memberNoPuedeEliminarProyectos() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, userId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).findByIdAndTenantId(any(), any());
        verify(repository, never()).delete(any(Project.class));
    }

    @Test
    void noEliminaUnProyectoDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, userId, Role.ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(Project.class));
    }
}
