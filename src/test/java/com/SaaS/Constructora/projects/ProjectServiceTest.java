package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.TenantQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
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
    private final ProjectService service = new ProjectService(repository, tenantQueryService);

    @Test
    void creaElProyectoConElTenantRecibido() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = service.create(tenantId,
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

        assertThrows(ResponseStatusException.class, () -> service.create(tenantId,
                new CreateProjectRequest("Torre Central", null, null)));

        verify(repository, never()).save(any(Project.class));
    }

    @Test
    void listaUsandoElFiltroPorTenant() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of());

        List<ProjectResponse> response = service.list(tenantId);

        assertEquals(List.of(), response);
        verify(repository).findByTenantIdOrderByCreatedAtDesc(tenantId);
        verifyNoInteractions(tenantQueryService);
    }
}
