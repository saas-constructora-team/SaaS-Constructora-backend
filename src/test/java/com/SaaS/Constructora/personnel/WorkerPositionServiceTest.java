package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerPositionServiceTest {

    private final WorkerPositionRepository positionRepository =
            mock(WorkerPositionRepository.class);
    private final WorkerRepository workerRepository = mock(WorkerRepository.class);
    private final PersonnelPermissionService permissionService =
            mock(PersonnelPermissionService.class);
    private final WorkerPositionService service =
            new WorkerPositionService(positionRepository, workerRepository, permissionService);

    @BeforeEach
    void setUp() {
        when(positionRepository.save(any(WorkerPosition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creaUnCargoEnElTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        WorkerPositionResponse response = service.create(tenantId, actorId,
                new CreateWorkerPositionRequest("Electricista", "Instalaciones eléctricas"));

        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
        assertEquals("Electricista", response.name());
        assertEquals("Instalaciones eléctricas", response.description());
        assertTrue(response.active());
    }

    @Test
    void noCreaUnCargoDuplicado() {
        UUID tenantId = UUID.randomUUID();
        when(positionRepository.existsByTenantIdAndName(tenantId, "Electricista"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(),
                        new CreateWorkerPositionRequest("Electricista", null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(positionRepository, never()).save(any(WorkerPosition.class));
    }

    @Test
    void listaLosCargosDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(positionRepository.findByTenantIdOrderByNameAsc(tenantId))
                .thenReturn(List.of(WorkerPosition.create(tenantId, "Operario", null)));

        List<WorkerPositionResponse> positions = service.list(tenantId, actorId);

        assertEquals(1, positions.size());
        assertEquals("Operario", positions.getFirst().name());
        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
    }

    @Test
    void actualizaElNombreYPropagaATrabajadores() {
        UUID tenantId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Operario", null);
        when(positionRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(position));
        when(positionRepository.existsByTenantIdAndNameAndIdNot(
                tenantId, "Oficial", id)).thenReturn(false);

        WorkerPositionResponse response = service.update(tenantId, UUID.randomUUID(), id,
                new UpdateWorkerPositionRequest("Oficial", null, null));

        assertEquals("Oficial", response.name());
        verify(workerRepository).renamePosition(tenantId, "Operario", "Oficial");
    }

    @Test
    void desactivaUnCargo() {
        UUID tenantId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Operario", null);
        when(positionRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(position));

        WorkerPositionResponse response = service.update(tenantId, UUID.randomUUID(), id,
                new UpdateWorkerPositionRequest(null, null, false));

        assertFalse(response.active());
    }

    @Test
    void noEliminaUnCargoConTrabajadoresAsignados() {
        UUID tenantId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Operario", null);
        when(positionRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(position));
        when(workerRepository.existsByTenantIdAndPositionName(tenantId, "Operario"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.delete(tenantId, UUID.randomUUID(), id));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(positionRepository, never()).delete(any(WorkerPosition.class));
    }

    @Test
    void eliminaUnCargoSinTrabajadores() {
        UUID tenantId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Operario", null);
        when(positionRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.of(position));
        when(workerRepository.existsByTenantIdAndPositionName(tenantId, "Operario"))
                .thenReturn(false);

        service.delete(tenantId, UUID.randomUUID(), id);

        verify(positionRepository).delete(position);
    }

    @Test
    void noAccedeAUnCargoDeOtroTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(positionRepository.findByTenantIdAndId(tenantId, id))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, UUID.randomUUID(), id,
                        new UpdateWorkerPositionRequest("Otro", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
