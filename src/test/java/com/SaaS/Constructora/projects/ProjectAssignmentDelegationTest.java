package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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

    private final ProjectAssignmentRepository repository =
            mock(ProjectAssignmentRepository.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final UserQueryService userQueryService = mock(UserQueryService.class);
    private final ProjectAssignmentService service =
            new ProjectAssignmentService(repository, projectService, userQueryService);

    @Test
    void adminPuedeNombrarLead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(user(userId, Role.MEMBER));
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, userId, ProjectRole.LEAD,
                actorId, Role.ADMIN);

        assertEquals(ProjectRole.LEAD, response.projectRole());
        assertEquals("MEMBER", response.userRole());
    }

    @Test
    void leadNoPuedeNombrarAOtroLead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.assign(tenantId, projectId, userId,
                        ProjectRole.LEAD, actorId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void leadPuedeAgregarUnMember() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(user(userId, Role.MEMBER));
        when(repository.save(any(ProjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, userId, ProjectRole.MEMBER,
                actorId, Role.MEMBER);

        assertEquals(ProjectRole.MEMBER, response.projectRole());
    }

    @Test
    void leadPuedeGestionarSiTieneRolLeadEnElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);

        service.requireCanManage(tenantId, projectId, actorId, Role.MEMBER);
    }

    @Test
    void memberSinRolLeadNoPuedeGestionarElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.MEMBER);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanManage(tenantId, projectId, actorId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void memberNoAsignadoNoPuedeGestionarElProyecto() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(repository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanManage(tenantId, projectId, actorId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void adminPuedeGestionarSinEstarAsignado() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        service.requireCanManage(tenantId, projectId, actorId, Role.ADMIN);

        verify(repository, never())
                .findByTenantIdAndProjectIdAndUserId(tenantId, projectId, actorId);
    }

    @Test
    void leadNoPuedeModificarAOtroLead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.LEAD);
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(user(userId, Role.MEMBER));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateRole(tenantId, projectId, userId,
                        ProjectRole.MEMBER, actorId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(ProjectAssignment.class));
    }

    @Test
    void leadNoPuedeQuitarAOtroLead() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.LEAD);
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(user(userId, Role.MEMBER));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.unassign(tenantId, projectId, userId,
                        actorId, Role.MEMBER));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never())
                .deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void leadPuedeQuitarAUnMember() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        mockProjectRole(tenantId, projectId, actorId, ProjectRole.LEAD);
        mockProjectRole(tenantId, projectId, userId, ProjectRole.MEMBER);
        when(userQueryService.ensureBelongsToTenant(userId, tenantId))
                .thenReturn(user(userId, Role.MEMBER));

        service.unassign(tenantId, projectId, userId, actorId, Role.MEMBER);

        verify(repository)
                .deleteByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId);
    }

    @Test
    void proyectoDeOtraEmpresaSeReportaComoInexistente() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectService).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanManage(tenantId, projectId, actorId, Role.MEMBER));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private void mockProjectRole(UUID tenantId, UUID projectId, UUID userId,
                                 ProjectRole role) {
        when(repository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId))
                .thenReturn(Optional.of(new ProjectAssignment(
                        tenantId, projectId, userId, role)));
    }

    private static UserSummary user(UUID id, Role role) {
        return new UserSummary(id, "miembro@demo.com", "Miembro Demo", role, true);
    }
}
