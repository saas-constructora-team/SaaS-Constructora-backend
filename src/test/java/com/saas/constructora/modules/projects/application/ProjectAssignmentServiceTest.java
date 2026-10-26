package com.saas.constructora.modules.projects;

import com.saas.constructora.modules.identity.application.*;
import com.saas.constructora.modules.identity.domain.*;
import com.saas.constructora.modules.projects.application.*;
import com.saas.constructora.modules.projects.domain.*;
import com.saas.constructora.modules.projects.infrastructure.*;
import com.saas.constructora.modules.projects.interfaces.dto.*;

import com.saas.constructora.modules.identity.application.UserQueryService;
import com.saas.constructora.modules.identity.application.UserSummary;
import com.saas.constructora.modules.identity.domain.Role;
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

class ProjectAssignmentServiceTest {

    private final ProjectAssignmentRepository repository =
            mock(ProjectAssignmentRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final UserQueryService userQueryService = mock(UserQueryService.class);
    private final ProjectAssignmentService service =
            new ProjectAssignmentService(repository, projectService, userQueryService);

    @Test
    void asignaUnUsuarioDelMismoTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserSummary user = new UserSummary(userId, "miembro@demo.com", "Miembro Demo",
                Role.MEMBER, true);

        when(userQueryService.ensureBelongsToTenant(userId, tenantId)).thenReturn(user);
        when(repository.existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(false);
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(tenantId, projectId, userId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        verify(userQueryService).ensureBelongsToTenant(userId, tenantId);
        assertEquals(projectId, response.projectId());
        assertEquals(userId, response.userId());
        assertEquals("Miembro Demo", response.userFullName());
        assertEquals("miembro@demo.com", response.userEmail());
        assertEquals("MEMBER", response.userRole());
    }

    @Test
    void noAsignaDosVecesAlMismoUsuario() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(new UserSummary(userId, "miembro@demo.com", "Miembro Demo",
                        Role.MEMBER, true));
        when(repository.existsByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void noAsignaSiElProyectoEsDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(userQueryService);
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void noAsignaSiElUsuarioEsDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void listaAsignadosConDatosDelUsuario() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ProjectAssignment assignment = new ProjectAssignment(tenantId, projectId, userId);
        when(userQueryService.listByTenant(tenantId)).thenReturn(List.of(
                new UserSummary(userId, "miembro@demo.com", "Miembro Demo",
                        Role.MEMBER, true)));
        when(repository.findByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId))
                .thenReturn(List.of(assignment));

        List<ProjectAssignmentResponse> response = service.list(tenantId, projectId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        assertEquals(1, response.size());
        assertEquals(userId, response.getFirst().userId());
        assertEquals("Miembro Demo", response.getFirst().userFullName());
    }

    @Test
    void noListaAsignacionesDeUnProyectoAjeno() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(tenantId, projectId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void desasignaUnUsuarioDelMismoTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        service.unassign(tenantId, projectId, userId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        verify(userQueryService).ensureBelongsToTenant(userId, tenantId);
        verify(repository).deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void desasignarEsIdempotente() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(repository.deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(0L);

        service.unassign(tenantId, projectId, userId);

        verify(repository).deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }
}
