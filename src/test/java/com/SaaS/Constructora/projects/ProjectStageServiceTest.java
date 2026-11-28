package com.SaaS.Constructora.projects;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.application.ProjectStageService;
import com.SaaS.Constructora.projects.domain.StageColor;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;
import com.SaaS.Constructora.projects.infrastructure.ProjectStageRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectStageRequest;
import com.SaaS.Constructora.projects.interfaces.ProjectStageResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectStageRequest;
import com.SaaS.Constructora.projects.interfaces.UpdateStageColorsRequest;

import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.events.ProjectStageClosed;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectStageServiceTest {

    private final ProjectStageRepository repository = mock(ProjectStageRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ProjectStageService service =
            new ProjectStageService(repository, projectService, events);

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

    @Test
    void cierraLaEtapaYPublicaElEventoDeDominio() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Construcción", 5);
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ProjectStageResponse response = service.close(
                tenantId, projectId, stage.getId(), userId, Role.MEMBER);

        assertEquals(stage.getId(), response.id());
        assertEquals("Construcción", response.name());
        assertEquals(5, response.order());
        assertNotNull(response.closedAt());
        assertTrue(stage.isClosed());

        ArgumentCaptor<ProjectStageClosed> eventCaptor =
                ArgumentCaptor.forClass(ProjectStageClosed.class);
        verify(events).publishEvent(eventCaptor.capture());
        ProjectStageClosed event = eventCaptor.getValue();
        assertEquals(tenantId, event.tenantId());
        assertEquals(projectId, event.projectId());
        assertEquals(stage.getId(), event.stageId());
        assertEquals(userId, event.actorUserId());
        assertEquals("Construcción", event.stageName());
        assertEquals(5, event.stageOrder());
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.MEMBER);
    }

    @Test
    void noCierraDosVecesLaMismaEtapa() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Construcción", 5);
        stage.close();
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.close(tenantId, projectId, stage.getId(), userId, Role.ADMIN));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(events);
    }

    @Test
    void noRenombraUnaEtapaCerrada() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Construcción", 5);
        stage.close();
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId, stage.getId(), userId, Role.ADMIN,
                        new UpdateProjectStageRequest("Otro nombre")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Construcción", stage.getName());
    }

    @Test
    void editaElColorYConservaElNombreYOrden() {
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Construcción", 5);
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ProjectStageResponse response = service.update(
                tenantId, projectId, stage.getId(), userId, Role.ADMIN,
                new UpdateProjectStageRequest("Obra gruesa", StageColor.TOSCA));

        assertEquals(StageColor.TOSCA, response.color());
        assertEquals("Obra gruesa", response.name());
        assertEquals(5, response.order());

        ArgumentCaptor<ProjectChanged> eventCaptor = ArgumentCaptor.forClass(ProjectChanged.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertEquals(ProjectChanged.STAGE, eventCaptor.getValue().section());
        assertEquals(projectId, eventCaptor.getValue().projectId());
    }

    @Test
    void colorAusenteConservaElColorAnterior() {
        ProjectStage stage = new ProjectStage(
                tenantId, projectId, "Construcción", 5, StageColor.LAVENDER);
        when(repository.findByIdAndTenantIdAndProjectId(stage.getId(), tenantId, projectId))
                .thenReturn(Optional.of(stage));

        ProjectStageResponse response = service.update(
                tenantId, projectId, stage.getId(), userId, Role.ADMIN,
                new UpdateProjectStageRequest("Obra gruesa"));

        assertEquals(StageColor.LAVENDER, response.color());
    }

    @Test
    void cambiaElColorDeVariasEtapasYPublicaUnAvisoPorProyecto() {
        UUID otherProjectId = UUID.randomUUID();
        ProjectStage first = new ProjectStage(tenantId, projectId, "Cimentación", 1);
        ProjectStage second = new ProjectStage(tenantId, otherProjectId, "Cimentación", 1);
        when(repository.findByIdAndTenantIdAndProjectId(first.getId(), tenantId, projectId))
                .thenReturn(Optional.of(first));
        when(repository.findByIdAndTenantIdAndProjectId(second.getId(), tenantId, otherProjectId))
                .thenReturn(Optional.of(second));

        service.updateColors(tenantId, userId, Role.ADMIN, new UpdateStageColorsRequest(List.of(
                new UpdateStageColorsRequest.StageColorUpdate(
                        projectId, first.getId(), StageColor.TOSCA),
                new UpdateStageColorsRequest.StageColorUpdate(
                        otherProjectId, second.getId(), StageColor.TOSCA))));

        assertEquals(StageColor.TOSCA, first.getColor());
        assertEquals(StageColor.TOSCA, second.getColor());
        verify(projectService).ensureUserCanAccessProject(projectId, tenantId, userId, Role.ADMIN);
        verify(projectService).ensureUserCanAccessProject(
                otherProjectId, tenantId, userId, Role.ADMIN);

        ArgumentCaptor<ProjectChanged> eventCaptor = ArgumentCaptor.forClass(ProjectChanged.class);
        verify(events, times(2)).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getAllValues().stream()
                .allMatch(event -> ProjectChanged.STAGE.equals(event.section())));
        assertEquals(Set.of(projectId, otherProjectId), eventCaptor.getAllValues().stream()
                .map(ProjectChanged::projectId).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void noCambiaNingunColorSiUnProyectoNoEsAccesible() {
        UUID otherProjectId = UUID.randomUUID();
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Cimentación", 1);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        otherProjectId, tenantId, userId, Role.MEMBER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateColors(tenantId, userId, Role.MEMBER,
                        new UpdateStageColorsRequest(List.of(
                                new UpdateStageColorsRequest.StageColorUpdate(
                                        projectId, stage.getId(), StageColor.TOSCA),
                                new UpdateStageColorsRequest.StageColorUpdate(
                                        otherProjectId, UUID.randomUUID(), StageColor.TOSCA)))));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals(StageColor.SAGE, stage.getColor());
        verifyNoInteractions(repository, events);
    }
}
