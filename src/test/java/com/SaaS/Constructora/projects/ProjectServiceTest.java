package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.TenantQueryService;
import com.SaaS.Constructora.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private final ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
    private final ProjectService service =
            new ProjectService(repository, tenantQueryService, assignmentRepository, stageRepository);

    @SuppressWarnings("unchecked")
    @Test
    void creaElProyectoConElTenantRecibidoYCreaLasEtapasIniciales() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.create(tenantId,
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
    }

    @Test
    void noGuardaSiLaEmpresaNoExiste() {
        UUID tenantId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no existe"))
                .when(tenantQueryService).ensureExists(tenantId);

        assertThrows(ResponseStatusException.class, () -> service.create(tenantId,
                new CreateProjectRequest("Torre Central", null, null)));

        verify(repository, never()).save(any(Project.class));
        verifyNoInteractions(stageRepository);
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
    void actualizaElProyectoDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = new Project(tenantId, "Nombre viejo", "Cliente viejo", "Ciudad vieja");
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));

        ProjectResponse response = service.update(tenantId, projectId,
                new UpdateProjectRequest("Nombre nuevo", "Cliente nuevo", "Ciudad nueva"));

        assertEquals(project.getId(), response.id());
        assertEquals(tenantId, response.tenantId());
        assertEquals("Nombre nuevo", response.name());
        assertEquals("Cliente nuevo", response.clientName());
        assertEquals("Ciudad nueva", response.location());
        assertEquals(ProjectStatus.ACTIVE, response.status());
    }

    @Test
    void noActualizaUnProyectoDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId,
                        new UpdateProjectRequest("Nombre nuevo", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eliminaElProyectoDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Project project = new Project(tenantId, "Proyecto", null, null);
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(project));

        service.delete(tenantId, projectId);

        verify(repository).delete(project);
    }

    @Test
    void noEliminaUnProyectoDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(Project.class));
    }
}
