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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reglas de delegación de la gestión de miembros de un proyecto:
 * un ADMIN puede nombrar LEAD y un LEAD solo puede administrar MEMBER.
 */
class ProjectAssignmentDelegationTest {

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

    private void mockProjectRole(UUID tenantId, UUID projectId, UUID userId, ProjectRole role) {
        when(repository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(Optional.of(new ProjectAssignment(tenantId, projectId, userId, role)));
    }

    @Test
    void adminPuedeNombrarLead() {
        UUID userId = UUID.randomUUID();
        actor(Role.ADMIN);
        targetUser(userId);
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, userId, ProjectRole.LEAD, actorId);

        assertEquals(ProjectRole.LEAD, response.projectRole());
        assertEquals("MEMBER", response.userRole());
    }

    @Test
    void leadNoPuedeNombrarAOtroLead() {
        UUID userId = UUID.randomUUID();
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId, ProjectRole.LEAD, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void leadPuedeAgregarUnMember() {
        UUID userId = UUID.randomUUID();
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        targetUser(userId);
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, userId, ProjectRole.MEMBER, actorId);

        assertEquals(ProjectRole.MEMBER, response.projectRole());
    }

    @Test
    void leadPuedeGestionarSiTieneRolLeadEnElProyecto() {
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);

        UserSummary authority = service.requireCanManage(tenantId, projectId, actorId);

        assertEquals(Role.MEMBER, authority.role());
    }

    @Test
    void memberSinRolLeadNoPuedeGestionarElProyecto() {
        actor(Role.MEMBER);
        when(repository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanManage(tenantId, projectId, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void memberNoAsignadoNoPuedeGestionarElProyecto() {
        actor(Role.MEMBER);
        when(repository.findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.list(tenantId, projectId, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void adminPuedeGestionarSinEstarAsignado() {
        actor(Role.ADMIN);

        service.requireCanManage(tenantId, projectId, actorId);

        verify(repository, never())
                .findActiveByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId);
    }

    @Test
    void leadNoPuedeModificarAOtroLead() {
        UUID userId = UUID.randomUUID();
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.LEAD);
        targetUser(userId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateRole(tenantId, projectId, userId, ProjectRole.MEMBER, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void leadNoPuedeQuitarAOtroLead() {
        UUID userId = UUID.randomUUID();
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.LEAD);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.unassign(tenantId, projectId, userId, actorId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).unassign(eq(tenantId), eq(projectId), eq(userId),
                any(), any(LocalDateTime.class));
    }

    @Test
    void leadPuedeQuitarAUnMember() {
        UUID userId = UUID.randomUUID();
        actor(Role.MEMBER);
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.MEMBER);

        service.unassign(tenantId, projectId, userId, actorId);

        verify(repository).unassign(eq(tenantId), eq(projectId), eq(userId),
                eq(actorId), any(LocalDateTime.class));
    }

    @Test
    void proyectoDeOtraEmpresaSeReportaComoInexistente() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanManage(tenantId, projectId, actorId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
