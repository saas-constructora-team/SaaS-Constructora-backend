package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import com.SaaS.Constructora.projects.ProjectService;
import org.junit.jupiter.api.BeforeEach;
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

class PersonnelServiceTest {

    private final WorkerRepository repository = mock(WorkerRepository.class);
    private final WorkerPositionRepository positionRepository =
            mock(WorkerPositionRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final PersonnelPermissionService permissionService =
            mock(PersonnelPermissionService.class);
    private final PersonnelService service =
            new PersonnelService(repository, positionRepository, projectService, permissionService);

    @BeforeEach
    void setUp() {
        when(positionRepository.save(any(WorkerPosition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreateWorkerRequest request(UUID projectId) {
        return new CreateWorkerRequest(
                "Ana Gómez Pérez",
                DocumentType.NATIONAL_ID,
                "1020304050",
                "+57 300 123 4567",
                "ana@constructora.com",
                "Carlos Gómez",
                "+57 300 765 4321",
                "Encargado de obra",
                projectId);
    }

    @Test
    void registraUnTrabajadorDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = service.create(tenantId, actorId, request(projectId));

        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        assertEquals("Ana Gómez Pérez", response.fullName());
        assertEquals("NATIONAL_ID", response.documentType());
        assertEquals("1020304050", response.documentNumber());
        assertEquals("Encargado de obra", response.position());
        assertEquals(projectId, response.projectId());
    }

    @Test
    void creaElCargoSiNoExisteEnElCatalogo() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(tenantId, UUID.randomUUID(), request(null));

        verify(positionRepository).save(any(WorkerPosition.class));
    }

    @Test
    void noAsignaUnCargoInactivo() {
        UUID tenantId = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Encargado de obra", null);
        position.activate(false);
        when(positionRepository.findByTenantIdAndName(tenantId, "Encargado de obra"))
                .thenReturn(Optional.of(position));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void permiteRegistrarSinObraAsignada() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = service.create(tenantId, UUID.randomUUID(), request(null));

        verifyNoInteractions(projectService);
        assertEquals(null, response.projectId());
    }

    @Test
    void noRegistraDocumentoDuplicadoEnLaEmpresa() {
        UUID tenantId = UUID.randomUUID();
        when(repository.existsByTenantIdAndDocumentTypeAndDocumentNumber(
                tenantId, DocumentType.NATIONAL_ID, "1020304050")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void noVinculaUnaObraDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(projectId)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void noRegistraSinPermisoDePersonal() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permisos"))
                .when(permissionService).requireCanManagePersonnel(tenantId, actorId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, actorId, request(null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void listaElPersonalDeLaEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Worker worker = new Worker(tenantId, "Ana Gómez", DocumentType.NATIONAL_ID,
                "123", "300", null, "Carlos", "301", "Operario", null);
        when(repository.findByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(worker));

        List<WorkerResponse> workers = service.list(tenantId, actorId);

        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
        assertEquals(1, workers.size());
        assertEquals("Ana Gómez", workers.getFirst().fullName());
    }
}
