package com.SaaS.Constructora.projects;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.infrastructure.Project;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
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
import com.SaaS.Constructora.shared.events.ProjectCreated;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private final ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
    private final TaskStatusRepository statusRepository = mock(TaskStatusRepository.class);
    private final ProjectAreaRepository areaRepository = mock(ProjectAreaRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ProjectService service = new ProjectService(
            repository, tenantQueryService, assignmentRepository, stageRepository,
            statusRepository, areaRepository, events);

    @SuppressWarnings("unchecked")
    @Test
    void creaElProyectoConElTenantRecibidoYCreaLasEtapasIniciales() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.create(tenantId, userId, Role.ADMIN,
                new CreateProjectRequest("Torre Central", "Inmobiliaria XYZ", "Bogotá"));

        verify(tenantQueryService).ensureExists(tenantId);
        assertEquals(tenantId, response.tenantId());
        assertEquals("Torre Central", response.name());
        assertEquals(ProjectStatus.ACTIVE, response.status());

        ArgumentCaptor<List<ProjectStage>> stagesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(stageRepository).saveAll(stagesCaptor.capture());
        List<ProjectStage> stages = stagesCaptor.getValue();
        assertEquals(6, stages.size());
        assertEquals("Estudios Preliminares y Diagnóstico", stages.getFirst().getName());
        assertEquals(1, stages.getFirst().getStageOrder());
        assertEquals("Cierre, Liquidación y Entrega", stages.getLast().getName());
        assertEquals(6, stages.getLast().getStageOrder());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TaskStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        verify(statusRepository).saveAll(statusesCaptor.capture());
        List<TaskStatus> statuses = statusesCaptor.getValue();
        assertEquals(3, statuses.size());
        assertEquals("Pendiente", statuses.getFirst().getName());
        assertTrue(statuses.getFirst().isDefaultStatus());
        assertEquals("Terminado", statuses.getLast().getName());
        assertTrue(statuses.getLast().isSystemStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectArea>> areasCaptor = ArgumentCaptor.forClass(List.class);
        verify(areaRepository).saveAll(areasCaptor.capture());
        List<ProjectArea> areas = areasCaptor.getValue();
        assertEquals(5, areas.size());
        assertEquals("Obra / Ingeniería", areas.getFirst().getName());
        assertEquals(AreaTipo.OBRA_INGENIERIA, areas.getFirst().getTipo());
        assertEquals(AreaTipo.CONTABILIDAD_FINANZAS, areas.getLast().getTipo());

        ArgumentCaptor<ProjectCreated> eventCaptor = ArgumentCaptor.forClass(ProjectCreated.class);
        verify(events).publishEvent(eventCaptor.capture());
        ProjectCreated event = eventCaptor.getValue();
        assertEquals(tenantId, event.tenantId());
        assertEquals(userId, event.actorUserId());
        assertEquals(response.id(), event.projectId());
        assertEquals("Torre Central", event.projectName());
    }

    @Test
    void noGuardaSiLaEmpresaNoExiste() {
        UUID tenantId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no existe"))
                .when(tenantQueryService).ensureExists(tenantId);

        assertThrows(ResponseStatusException.class, () -> service.create(
                tenantId, UUID.randomUUID(), Role.ADMIN,
                new CreateProjectRequest("Torre Central", null, null)));

        verify(repository, never()).save(any(Project.class));
        verifyNoInteractions(stageRepository);
        verifyNoInteractions(statusRepository);
        verifyNoInteractions(events);
    }

    @Test
    void memberNoPuedeCrearProyectos() {
        UUID tenantId = UUID.randomUUID();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), Role.MEMBER,
                        new CreateProjectRequest("Torre Central", null, null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(tenantQueryService);
        verify(repository, never()).save(any(Project.class));
        verifyNoInteractions(stageRepository);
        verifyNoInteractions(statusRepository);
        verifyNoInteractions(events);
    }

    @Test
    void adminListaTodosLosProyectosDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findVisibleProjects(tenantId, userId, true)).thenReturn(List.of());

        List<ProjectResponse> response = service.list(tenantId, userId, Role.ADMIN);

        assertEquals(List.of(), response);
        verify(repository).findVisibleProjects(tenantId, userId, true);
        verifyNoInteractions(tenantQueryService);
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void memberSoloListaSusProyectosAsignados() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project assigned = new Project(tenantId, "Proyecto asignado", null, null);
        when(repository.findVisibleProjects(tenantId, userId, false))
                .thenReturn(List.of(assigned));

        List<ProjectResponse> response = service.list(tenantId, userId, Role.MEMBER);

        assertEquals(1, response.size());
        assertEquals("Proyecto asignado", response.getFirst().name());
        verify(repository).findVisibleProjects(tenantId, userId, false);
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void busquedaAplicaFiltrosYPaginacionDentroDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project assigned = new Project(tenantId, "Torre Central", null, null);
        when(repository.searchProjects(eq(tenantId), eq(userId), eq(false), eq("%torre%"),
                eq(ProjectStatus.ACTIVE), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assigned)));

        Page<ProjectResponse> page = service.search(tenantId, userId, Role.MEMBER, "  Torre  ",
                ProjectStatus.ACTIVE, null, null, null, PageRequest.of(0, 5));

        assertEquals(1, page.getTotalElements());
        assertEquals("Torre Central", page.getContent().getFirst().name());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).searchProjects(eq(tenantId), eq(userId), eq(false), eq("%torre%"),
                eq(ProjectStatus.ACTIVE), eq(null), eq(null), eq(null), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC,
                pageable.getSort().getOrderFor("createdAt").getDirection());
        verifyNoInteractions(assignmentRepository);
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
                .existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberAsignadoPuedeAccederAlProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));
        when(assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId)).thenReturn(true);

        service.ensureUserCanAccessProject(projectId, tenantId, userId, Role.MEMBER);

        verify(assignmentRepository)
                .existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberNoAsignadoNoPuedeAccederAlProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId))
                .thenReturn(Optional.of(new Project(tenantId, "Proyecto", null, null)));
        when(assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
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
                .existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void memberAsignadoActualizaElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Project project = new Project(tenantId, "Nombre viejo", null, null);
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));
        when(assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
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
        when(assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
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
