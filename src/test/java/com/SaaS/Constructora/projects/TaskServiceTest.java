package com.SaaS.Constructora.projects;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.application.TaskService;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.domain.CapituloPresupuestal;
import com.SaaS.Constructora.projects.domain.CategoriaInsumo;
import com.SaaS.Constructora.projects.domain.Disciplina;
import com.SaaS.Constructora.projects.domain.PermisoSst;
import com.SaaS.Constructora.projects.domain.TipoDocumentoContable;
import com.SaaS.Constructora.projects.domain.UnidadMedida;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

class TaskServiceTest {

    private final TaskRepository repository = mock(TaskRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final TaskCreationPermissionService permissionService =
            mock(TaskCreationPermissionService.class);
    private final TaskStatusRepository statusRepository = mock(TaskStatusRepository.class);
    private final ProjectAreaRepository areaRepository = mock(ProjectAreaRepository.class);
    private final ProjectAssignmentRepository assignmentRepository =
            mock(ProjectAssignmentRepository.class);
    private final ProjectStageRepository stageRepository = mock(ProjectStageRepository.class);
    private final TaskService service = new TaskService(repository, projectService, permissionService,
            statusRepository, areaRepository, assignmentRepository, stageRepository);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private TaskStatus status() {
        return new TaskStatus(tenantId, projectId, "Pendiente", "#EF4444", 1, true);
    }

    private ProjectArea area() {
        return new ProjectArea(tenantId, projectId, "Cimentación");
    }

    private ProjectArea area(AreaTipo tipo) {
        return new ProjectArea(tenantId, projectId, "Área " + tipo, tipo);
    }

    private ProjectAssignment assignment(UUID assignedUserId) {
        return new ProjectAssignment(tenantId, projectId, assignedUserId);
    }

    @Test
    void creaLaTareaConElEstadoPorDefectoDelProyecto() {
        TaskStatus status = status();
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = service.create(
                tenantId, projectId, userId, Role.WORKER,
                new CreateTaskRequest("Revisar planos", "Revisar la estructura"));

        assertEquals(projectId, response.projectId());
        assertEquals("Revisar planos", response.title());
        assertEquals("Pendiente", response.status().name());
        assertEquals("#EF4444", response.status().color());
        assertTrue(response.status().isDefault());
        assertNull(response.area());
        assertNull(response.assignee());
        assertNull(response.validator());
        assertEquals(userId, response.createdBy());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(tenantId, captor.getValue().getTenantId());
        assertEquals(status.getId(), captor.getValue().getStatusId());
        verify(permissionService).requireCanCreateTasks(tenantId, userId);
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.WORKER);
    }

    @Test
    void noCreaSiNoTienePermisoDeCreacion() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para crear tareas"))
                .when(permissionService).requireCanCreateTasks(tenantId, userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateTaskRequest("Tarea", null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(projectService, never()).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.WORKER);
        verifyNoInteractions(repository, statusRepository, areaRepository, assignmentRepository);
    }

    @Test
    void noCreaSiNoPuedeAccederAlProyecto() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.WORKER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateTaskRequest("Tarea", null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, statusRepository, areaRepository, assignmentRepository);
    }

    @Test
    void creaLaTareaConEstadoAreaAsignadoYValidadorIndicados() {
        TaskStatus status = status();
        ProjectArea area = area();
        UUID assigneeUserId = UUID.randomUUID();
        UUID validatorUserId = UUID.randomUUID();
        ProjectAssignment assignee = assignment(assigneeUserId);
        ProjectAssignment validator = assignment(validatorUserId);

        when(statusRepository.findByIdAndTenantIdAndProjectId(status.getId(), tenantId, projectId))
                .thenReturn(Optional.of(status));
        when(areaRepository.findByIdAndTenantIdAndProjectId(area.getId(), tenantId, projectId))
                .thenReturn(Optional.of(area));
        when(assignmentRepository.findActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, assigneeUserId)).thenReturn(Optional.of(assignee));
        when(assignmentRepository.findActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, validatorUserId)).thenReturn(Optional.of(validator));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = service.create(tenantId, projectId, userId, Role.WORKER,
                new CreateTaskRequest("Colocar encofrado", null, status.getId(), area.getId(),
                        assigneeUserId, validatorUserId));

        assertEquals(status.getId(), response.status().id());
        assertEquals(area.getId(), response.area().id());
        assertEquals("Cimentación", response.area().name());
        assertEquals(assignee.getId(), response.assignee().assignmentId());
        assertEquals(assigneeUserId, response.assignee().userId());
        assertEquals(validator.getId(), response.validator().assignmentId());
        assertEquals(validatorUserId, response.validator().userId());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(area.getId(), captor.getValue().getAreaId());
        assertEquals(assignee.getId(), captor.getValue().getAssigneeAssignmentId());
        assertEquals(validator.getId(), captor.getValue().getValidatorAssignmentId());
    }

    @Test
    void noCreaSiElEstadoNoPerteneceAlProyecto() {
        UUID statusId = UUID.randomUUID();
        when(statusRepository.findByIdAndTenantIdAndProjectId(statusId, tenantId, projectId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateTaskRequest("Tarea", null, statusId, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, areaRepository, assignmentRepository);
    }

    @Test
    void noCreaSiElUsuarioNoEstaAsignadoAlProyecto() {
        UUID assigneeUserId = UUID.randomUUID();
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));
        when(assignmentRepository.findActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, assigneeUserId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.WORKER,
                        new CreateTaskRequest("Tarea", null, null, null, assigneeUserId, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, areaRepository);
    }

    @Test
    void listaLasTareasConEstadoAreaYUsuariosResueltos() {
        TaskStatus status = status();
        ProjectArea area = area();
        ProjectAssignment assignee = assignment(UUID.randomUUID());
        ProjectAssignment validator = assignment(UUID.randomUUID());
        Task task = new Task(tenantId, projectId, userId, "Tarea 1", "Descripción",
                status.getId(), area.getId(), assignee.getId(), validator.getId());

        when(repository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId))
                .thenReturn(List.of(task));
        when(statusRepository.findByTenantIdAndProjectIdOrderByStatusOrderAsc(tenantId, projectId))
                .thenReturn(List.of(status));
        when(areaRepository.findByTenantIdAndProjectIdOrderByNameAsc(tenantId, projectId))
                .thenReturn(List.of(area));
        when(assignmentRepository.findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId))
                .thenReturn(List.of(assignee, validator));

        List<TaskResponse> response = service.list(
                tenantId, projectId, userId, Role.ADMIN);

        assertEquals(1, response.size());
        assertEquals("Tarea 1", response.getFirst().title());
        assertEquals("Pendiente", response.getFirst().status().name());
        assertEquals("Cimentación", response.getFirst().area().name());
        assertEquals(assignee.getUserId(), response.getFirst().assignee().userId());
        assertEquals(validator.getUserId(), response.getFirst().validator().userId());
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.ADMIN);
    }

    @Test
    void creaLaTareaConFechaPrevistaYDetallesDeObra() {
        ProjectArea area = area(AreaTipo.OBRA_INGENIERIA);
        UUID contratistaId = UUID.randomUUID();
        LocalDate fechaFin = LocalDate.of(2026, 12, 31);
        AreaDetailsRequest details = new AreaDetailsRequest(
                "Torre 1 > Piso 3 > Apto 301", CapituloPresupuestal.CIMENTACION, UnidadMedida.M2,
                new BigDecimal("120.500"), contratistaId, true,
                Set.of(PermisoSst.TRABAJO_EN_ALTURAS),
                null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));
        when(areaRepository.findByIdAndTenantIdAndProjectId(area.getId(), tenantId, projectId))
                .thenReturn(Optional.of(area));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = service.create(tenantId, projectId, userId, Role.ADMIN,
                new CreateTaskRequest("Colocar encofrado", null, null, area.getId(), null, null,
                        fechaFin, details));

        assertEquals(fechaFin, response.fechaFinPrevista());
        assertEquals(AreaTipo.OBRA_INGENIERIA, response.area().tipo());
        assertEquals("Torre 1 > Piso 3 > Apto 301", response.areaDetails().frenteObra());
        assertEquals(CapituloPresupuestal.CIMENTACION, response.areaDetails().capituloPresupuestal());
        assertEquals(UnidadMedida.M2, response.areaDetails().unidadMedida());
        assertEquals(0, new BigDecimal("120.500").compareTo(response.areaDetails().cantidadProgramada()));
        assertEquals(contratistaId, response.areaDetails().contratistaId());
        assertEquals(List.of(PermisoSst.TRABAJO_EN_ALTURAS), response.areaDetails().permisosSst());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(fechaFin, captor.getValue().getFechaFinPrevista());
        assertEquals(CapituloPresupuestal.CIMENTACION,
                captor.getValue().getDetallesArea().getCapituloPresupuestal());
    }

    @Test
    void creaLaTareaConDetallesDeCompras() {
        ProjectArea area = area(AreaTipo.COMPRAS_ALMACEN);
        LocalDate fechaRequerida = LocalDate.of(2026, 11, 5);
        AreaDetailsRequest details = new AreaDetailsRequest(
                null, null, null, null, null, null, null,
                "REQ-102", fechaRequerida, CategoriaInsumo.MATERIALES,
                new BigDecimal("1500000.00"), "Proveedor Ejemplo",
                null, null, null, null, null, null, null, null);
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));
        when(areaRepository.findByIdAndTenantIdAndProjectId(area.getId(), tenantId, projectId))
                .thenReturn(Optional.of(area));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = service.create(tenantId, projectId, userId, Role.ADMIN,
                new CreateTaskRequest("Pedir materiales", null, null, area.getId(), null, null,
                        null, details));

        assertEquals("REQ-102", response.areaDetails().codigoRequisicion());
        assertEquals(fechaRequerida, response.areaDetails().fechaRequeridaEnObra());
        assertEquals(CategoriaInsumo.MATERIALES, response.areaDetails().categoriaInsumo());
        assertEquals("Proveedor Ejemplo", response.areaDetails().proveedorSugerido());
        assertEquals(0, new BigDecimal("1500000.00")
                .compareTo(response.areaDetails().presupuestoEstimado()));
    }

    @Test
    void rechazaCamposQueNoPertenecenAlAreaElegida() {
        ProjectArea area = area(AreaTipo.DISENO_ARQUITECTURA);
        AreaDetailsRequest details = new AreaDetailsRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                "Curaduría", null, null, null, null);
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));
        when(areaRepository.findByIdAndTenantIdAndProjectId(area.getId(), tenantId, projectId))
                .thenReturn(Optional.of(area));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.ADMIN,
                        new CreateTaskRequest("Tarea", null, null, area.getId(), null, null,
                                null, details)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rechazaDetallesDeAreaSinAreaSeleccionada() {
        AreaDetailsRequest details = new AreaDetailsRequest(null, null, null, null, null, null,
                null, null, null, null, null, null, Disciplina.ESTRUCTURAL,
                null, null, null, null, null, null, null);
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.ADMIN,
                        new CreateTaskRequest("Tarea", null, null, null, null, null, null, details)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository, areaRepository);
    }

    @Test
    void listaLasTareasConDetallesDelArea() {
        TaskStatus status = status();
        ProjectArea area = area(AreaTipo.CONTABILIDAD_FINANZAS);
        Task task = new Task(tenantId, projectId, userId, "Factura", null, null,
                new TaskAreaDetails(
                        null, null, null, null, null, false, null, null, null, null, null, null,
                        null, null, null, null, null, TipoDocumentoContable.FACTURA, "F-123",
                        new BigDecimal("250000.00")),
                status.getId(), area.getId(), null, null);
        when(repository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId))
                .thenReturn(List.of(task));
        when(statusRepository.findByTenantIdAndProjectIdOrderByStatusOrderAsc(tenantId, projectId))
                .thenReturn(List.of(status));
        when(areaRepository.findByTenantIdAndProjectIdOrderByNameAsc(tenantId, projectId))
                .thenReturn(List.of(area));

        List<TaskResponse> response = service.list(tenantId, projectId, userId, Role.ADMIN);

        assertEquals(1, response.size());
        assertEquals(TipoDocumentoContable.FACTURA, response.getFirst().areaDetails().tipoDocumento());
        assertEquals("F-123", response.getFirst().areaDetails().numeroDocumento());
    }

    @Test
    void noListaSiNoTieneAccesoAlProyecto() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        projectId, tenantId, userId, Role.WORKER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(tenantId, projectId, userId, Role.WORKER));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, statusRepository, areaRepository, assignmentRepository);
    }

    @Test
    void creaLaTareaVinculadaAUnaEtapa() {
        UUID stageId = UUID.randomUUID();
        when(statusRepository.findByTenantIdAndProjectIdAndDefaultStatusTrue(tenantId, projectId))
                .thenReturn(Optional.of(status()));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = service.create(tenantId, projectId, userId, Role.ADMIN,
                new CreateTaskRequest("Tarea en etapa", null, null, null, null, null,
                        null, null, stageId));

        assertEquals(stageId, response.stageId());
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(stageId, captor.getValue().getStageId());
        verify(projectService).ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        verify(projectService).ensureStageAllowsEntries(stageId);
    }

    @Test
    void noCreaSiLaEtapaNoPerteneceAlProyecto() {
        UUID stageId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa no encontrada"))
                .when(projectService).ensureStageBelongsToProjectAndTenant(
                        stageId, projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.ADMIN,
                        new CreateTaskRequest("Tarea", null, null, null, null, null,
                                null, null, stageId)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, statusRepository, areaRepository, assignmentRepository);
    }

    @Test
    void noCreaSiLaEtapaEstaCerrada() {
        UUID stageId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "La etapa está cerrada, no permite registrar novedades"))
                .when(projectService).ensureStageAllowsEntries(stageId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, userId, Role.ADMIN,
                        new CreateTaskRequest("Tarea", null, null, null, null, null,
                                null, null, stageId)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(repository, statusRepository, areaRepository, assignmentRepository);
    }

    private Task tareaConEtapa(UUID stageId, TaskStatus status) {
        return new Task(tenantId, projectId, userId, "Tarea en etapa", null, null,
                new TaskAreaDetails(), status.getId(), null, null, null, stageId);
    }

    @Test
    void mueveLaTareaAUnaEtapaDelMismoProyecto() {
        UUID taskId = UUID.randomUUID();
        UUID targetStageId = UUID.randomUUID();
        TaskStatus status = status();
        when(repository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.of(tareaConEtapa(UUID.randomUUID(), status)));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(statusRepository.findByIdAndTenantIdAndProjectId(
                status.getId(), tenantId, projectId)).thenReturn(Optional.of(status));

        TaskResponse response = service.moveToStage(
                tenantId, taskId, userId, Role.WORKER, targetStageId);

        assertEquals(targetStageId, response.stageId());
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(repository).save(captor.capture());
        assertEquals(targetStageId, captor.getValue().getStageId());
        verify(permissionService).requireCanCreateTasks(tenantId, userId);
        verify(projectService).ensureUserCanAccessProject(
                projectId, tenantId, userId, Role.WORKER);
        verify(projectService).ensureStageBelongsToProjectAndTenant(
                targetStageId, projectId, tenantId);
        verify(projectService).ensureStageAllowsEntries(targetStageId);
    }

    @Test
    void mueveLaTareaASinEtapaSinValidarEtapaDestino() {
        UUID taskId = UUID.randomUUID();
        TaskStatus status = status();
        when(repository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.of(tareaConEtapa(UUID.randomUUID(), status)));
        when(repository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(statusRepository.findByIdAndTenantIdAndProjectId(
                status.getId(), tenantId, projectId)).thenReturn(Optional.of(status));

        TaskResponse response = service.moveToStage(tenantId, taskId, userId, Role.ADMIN, null);

        assertNull(response.stageId());
        verify(projectService, never()).ensureStageBelongsToProjectAndTenant(
                any(), any(), any());
        verify(projectService, never()).ensureStageAllowsEntries(any());
    }

    @Test
    void noMueveUnaTareaDeOtroTenant() {
        UUID taskId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.moveToStage(tenantId, taskId, userId, Role.ADMIN, UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(projectService, statusRepository, areaRepository, assignmentRepository);
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void noMueveLaTareaAUnaEtapaDeOtroProyecto() {
        UUID taskId = UUID.randomUUID();
        UUID foreignStageId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.of(tareaConEtapa(null, status())));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa no encontrada"))
                .when(projectService).ensureStageBelongsToProjectAndTenant(
                        foreignStageId, projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.moveToStage(tenantId, taskId, userId, Role.ADMIN, foreignStageId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void noMueveLaTareaAUnaEtapaCerrada() {
        UUID taskId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        when(repository.findByIdAndTenantId(taskId, tenantId))
                .thenReturn(Optional.of(tareaConEtapa(null, status())));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La etapa está cerrada"))
                .when(projectService).ensureStageAllowsEntries(stageId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.moveToStage(tenantId, taskId, userId, Role.ADMIN, stageId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Task.class));
    }

    @Test
    void tableroDevuelveEtapasYTareasDeLosProyectosVisibles() {
        UUID stageId = UUID.randomUUID();
        ProjectStage stage = new ProjectStage(tenantId, projectId, "Cimentación", 1);
        TaskStatus status = status();
        Task task = new Task(tenantId, projectId, userId, "Tarea en etapa", null,
                null, null, status.getId(), null, null, null, stageId);

        when(projectService.listVisibleProjectIds(tenantId, userId, Role.ADMIN))
                .thenReturn(List.of(projectId));
        when(repository.findByTenantIdAndProjectIdIn(
                eq(tenantId), eq(List.of(projectId)), any(Pageable.class)))
                .thenReturn(List.of(task));
        when(stageRepository.findByTenantIdAndProjectIdInOrderByStageOrderAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of(stage));
        when(statusRepository.findByTenantIdAndProjectIdInOrderByStatusOrderAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of(status));
        when(areaRepository.findByTenantIdAndProjectIdInOrderByNameAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of());
        when(assignmentRepository.findActiveByTenantIdAndProjectIdIn(tenantId, List.of(projectId)))
                .thenReturn(List.of());

        TaskBoardResponse board = service.getBoard(tenantId, userId, Role.ADMIN, null, 500);

        assertEquals(1, board.stages().size());
        assertEquals("Cimentación", board.stages().getFirst().name());
        assertEquals(1, board.tasks().size());
        assertEquals(stageId, board.tasks().getFirst().stageId());
        assertEquals("Pendiente", board.tasks().getFirst().status().name());
        assertFalse(board.truncated());
        verify(projectService).listVisibleProjectIds(tenantId, userId, Role.ADMIN);
    }

    @Test
    void tableroSinProyectosVisiblesDevuelveListasVacias() {
        when(projectService.listVisibleProjectIds(tenantId, userId, Role.WORKER))
                .thenReturn(List.of());

        TaskBoardResponse board = service.getBoard(tenantId, userId, Role.WORKER, null, 500);

        assertEquals(List.of(), board.stages());
        assertEquals(List.of(), board.tasks());
        assertFalse(board.truncated());
        verifyNoInteractions(repository, statusRepository, areaRepository,
                assignmentRepository, stageRepository);
    }

    @Test
    void tableroConProyectoValidaAccesoYRecortaSegunElLimite() {
        UUID otherProjectId = UUID.randomUUID();
        TaskStatus status = status();
        Task first = new Task(tenantId, projectId, userId, "Reciente", null,
                null, null, status.getId(), null, null, null, null);
        Task second = new Task(tenantId, projectId, userId, "Anterior", null,
                null, null, status.getId(), null, null, null, null);

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureUserCanAccessProject(
                        otherProjectId, tenantId, userId, Role.WORKER);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.getBoard(tenantId, userId, Role.WORKER, otherProjectId, 500));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository, statusRepository, areaRepository,
                assignmentRepository, stageRepository);

        when(repository.findByTenantIdAndProjectIdIn(
                eq(tenantId), eq(List.of(projectId)), any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(statusRepository.findByTenantIdAndProjectIdInOrderByStatusOrderAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of(status));
        when(areaRepository.findByTenantIdAndProjectIdInOrderByNameAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of());
        when(assignmentRepository.findActiveByTenantIdAndProjectIdIn(tenantId, List.of(projectId)))
                .thenReturn(List.of());
        when(stageRepository.findByTenantIdAndProjectIdInOrderByStageOrderAsc(tenantId, List.of(projectId)))
                .thenReturn(List.of());

        TaskBoardResponse board = service.getBoard(tenantId, userId, Role.WORKER, projectId, 1);

        assertEquals(1, board.tasks().size());
        assertEquals("Reciente", board.tasks().getFirst().title());
        assertTrue(board.truncated());
        verify(projectService).ensureUserCanAccessProject(projectId, tenantId, userId, Role.WORKER);
    }
}
