package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.domain.Role;
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

class ProjectStageServiceTest {

    private final ProjectStageRepository repository = mock(ProjectStageRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectStageService service =
            new ProjectStageService(repository, projectService);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void listaEtapasOrdenadasDeUnProyectoAccesible() {
        ProjectStage first = new ProjectStage(tenantId, projectId, "Anteproyecto", 1);
        ProjectStage second = new ProjectStage(tenantId, projectId, "Construcción", 2);
        when(repository.findByTenantIdAndProjectIdOrderByStageOrderAsc(tenantId, projectId))
                .thenReturn(List.of(first, second));

        List<ProjectStageResponse> response = service.list(
                tenantId, projectId, userId, Role.ADMIN);

        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.ADMIN);
        assertEquals(2, response.size());
        assertEquals("Anteproyecto", response.getFirst().name());
        assertEquals(1, response.getFirst().order());
        assertEquals("Construcción", response.getLast().name());
        assertEquals(2, response.getLast().order());
    }

    @Test
    void noListaSiNoTieneAccesoAlProyecto() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.MEMBER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(tenantId, projectId, userId, Role.MEMBER));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void creaLaEtapaAlFinalDelOrden() {
        when(repository.findMaxStageOrder(tenantId, projectId)).thenReturn(6);
        when(repository.save(any(ProjectStage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectStageResponse response = service.create(
                tenantId, projectId, userId, Role.ADMIN,
                new CreateProjectStageRequest("Etapa adicional"));

        assertEquals("Etapa adicional", response.name());
        assertEquals(7, response.order());
        assertEquals(projectId, response.projectId());
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.ADMIN);
    }

    @Test
    void noCreaSiNoTieneAccesoAlProyecto() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.MEMBER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.MEMBER,
                        new CreateProjectStageRequest("Etapa adicional")));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void editaElNombreYConservaElOrden() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Nombre viejo", 3);
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ProjectStageResponse response = service.update(
                tenantId, projectId, stage.getId(), userId, Role.ADMIN,
                new UpdateProjectStageRequest("Nombre nuevo"));

        assertEquals("Nombre nuevo", response.name());
        assertEquals(3, response.order());
        assertEquals(stage.getId(), response.id());
    }

    @Test
    void noEditaUnaEtapaDeOtroProyecto() {
        UUID stageId = UUID.randomUUID();
        when(repository.findByIdAndTenantIdAndProjectId(stageId, tenantId, projectId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId, stageId, userId, Role.ADMIN,
                        new UpdateProjectStageRequest("Nombre nuevo")));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eliminaUnaEtapaDelProyecto() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Etapa", 1);
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        service.delete(tenantId, projectId, stage.getId(), userId, Role.ADMIN);

        verify(repository).delete(stage);
    }

    @Test
    void noEliminaUnaEtapaInexistente() {
        UUID stageId = UUID.randomUUID();
        when(repository.findByIdAndTenantIdAndProjectId(stageId, tenantId, projectId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, projectId, stageId, userId, Role.ADMIN));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).delete(any(ProjectStage.class));
    }
}
