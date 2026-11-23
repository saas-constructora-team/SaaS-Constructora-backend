package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ProjectAssignmentService;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import com.SaaS.Constructora.projects.domain.ProjectRole;
import com.SaaS.Constructora.projects.interfaces.ProjectAssignmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectAssignmentServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private final ProjectAssignmentRepository repository = mock(ProjectAssignmentRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final UserQueryService userQueryService = mock(UserQueryService.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ProjectAssignmentService service = new ProjectAssignmentService(
            repository, projectService, userQueryService, events);

    private void actor(Role role) {
        when(userQueryService.ensureBelongsToTenant(actorId, tenantId))
                .thenReturn(new UserSummary(actorId, "actor@demo.com", "Actor", role, true));
    }

    private void targetUser(UUID userId) {
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(new UserSummary(userId, "miembro@demo.com", "Miembro Demo",
                        Role.MEMBER, true));
    }

    @Test
    void asignaUnUsuarioDelMismoTenant() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        targetUser(userId);
        when(repository.existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(false);
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, userId, ProjectRole.MEMBER, actorId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        verify(userQueryService).ensureBelongsToTenant(userId, tenantId);
        assertEquals(projectId, response.projectId());
        assertEquals(userId, response.userId());
        assertEquals("Miembro Demo", response.userFullName());
        assertEquals("MEMBER", response.userRole());
    }

    @Test
    void noAsignaDosVecesAlMismoUsuario() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        targetUser(userId);
        when(repository.existsActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId, ProjectRole.MEMBER, actorId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void noAsignaSiElProyectoEsDeOtraEmpresa() {
        UUID userId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId, ProjectRole.MEMBER, actorId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(userQueryService);
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void noAsignaSiElUsuarioEsDeOtraEmpresa() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId, ProjectRole.MEMBER, actorId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void miembroConTokenDeAdminPeroSinDelegacionNoPuedeAsignar() {
        UUID userId = UUID.randomUUID();
        // El actor era ADMIN cuando se emitió el token, pero la BD dice MEMBER.
        actor(Role.MEMBER);
        when(repository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId))
                .thenReturn(java.util.Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId, ProjectRole.MEMBER, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void listaAsignadosConDatosDelUsuarioEnUnaSolaConsulta() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        ProjectAssignment assignment = new ProjectAssignment(tenantId, projectId, userId);
        when(repository.findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(tenantId, projectId))
                .thenReturn(List.of(assignment));
        when(userQueryService.listByTenant(tenantId, Set.of(userId))).thenReturn(List.of(
                new UserSummary(userId, "miembro@demo.com", "Miembro Demo",
                        Role.MEMBER, true)));

        List<ProjectAssignmentResponse> response = service.list(tenantId, projectId, actorId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        assertEquals(1, response.size());
        assertEquals(userId, response.getFirst().userId());
        assertEquals("Miembro Demo", response.getFirst().userFullName());
    }

    @Test
    void noListaAsignacionesDeUnProyectoAjeno() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(tenantId, projectId, actorId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void desasignaUnUsuarioDelMismoTenant() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);

        service.unassign(tenantId, projectId, userId, actorId);

        verify(projectService).ensureBelongsToTenant(projectId, tenantId);
        verify(repository).unassign(eq(tenantId), eq(projectId), eq(userId),
                eq(actorId), any(LocalDateTime.class));
    }

    @Test
    void desasignarEsIdempotente() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        when(repository.unassign(eq(tenantId), eq(projectId), eq(userId),
                any(), any(LocalDateTime.class))).thenReturn(0);

        service.unassign(tenantId, projectId, userId, actorId);

        verify(repository).unassign(eq(tenantId), eq(projectId), eq(userId),
                eq(actorId), any(LocalDateTime.class));
    }

    @Test
    void revocarMembresiaRetiraTodasLasAsignaciones() {
        UUID userId = UUID.randomUUID();
        ProjectAssignment assignment = new ProjectAssignment(tenantId, projectId, userId);
        when(repository.findActiveByTenantIdAndUserId(tenantId, userId))
                .thenReturn(List.of(assignment));

        service.unassignAllForUser(tenantId, userId, actorId);

        verify(repository).unassignAllForUser(eq(tenantId), eq(userId),
                eq(actorId), any(LocalDateTime.class));
        verify(events).publishEvent(any(com.SaaS.Constructora.shared.events.ProjectAssignmentChanged.class));
    }
}
