package com.SaaS.Constructora.sitelog;

import com.SaaS.Constructora.identity.application.SiteLogManagementPermissionService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.application.ports.UserQueryPort;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.sitelog.application.SiteLogRecordService;
import com.SaaS.Constructora.sitelog.application.ports.SiteLogRecordRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogStatus;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import com.SaaS.Constructora.sitelog.interfaces.SiteLogRecordRequest;
import com.SaaS.Constructora.sitelog.interfaces.SiteLogRecordResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteLogRecordServiceTest {

    private final SiteLogRecordRepository repository = mock(SiteLogRecordRepository.class);
    private final ProjectTenantPort projects = mock(ProjectTenantPort.class);
    private final UserQueryPort users = mock(UserQueryPort.class);
    private final SiteLogManagementPermissionService permissions =
            mock(SiteLogManagementPermissionService.class);

    private SiteLogRecordService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final UUID assigneeId = UUID.randomUUID();
    private final AuthenticatedUser author = new AuthenticatedUser(authorId, tenantId, Role.MEMBER);

    @BeforeEach
    void setUp() {
        service = new SiteLogRecordService(repository, projects, users, permissions);
        when(projects.isUserAssignedToProject(projectId, tenantId, authorId)).thenReturn(true);
        when(projects.getProjectName(projectId, tenantId)).thenReturn("Obra Torres");
        when(users.ensureBelongsToTenant(authorId, tenantId))
                .thenReturn(summary(authorId, "Ana Autora"));
        when(users.ensureBelongsToTenant(assigneeId, tenantId))
                .thenReturn(summary(assigneeId, "Bruno Responsable"));
        when(repository.save(any(SiteLogRecord.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void creaLaEntradaConAutorCalculadoYNombreDeProyecto() {
        SiteLogRecordResponse response = service.create(tenantId, projectId, author, request());

        assertEquals(projectId, response.projectId());
        assertEquals("Obra Torres", response.projectName());
        assertEquals("Ana Autora", response.author());
        assertEquals("Bruno Responsable", response.assigneeName());
        assertEquals(0, response.photos());
        assertEquals(SiteLogType.RETRASO, response.type());
        assertEquals("  Se colocó  ".trim(), response.what());
        verify(projects).ensureBelongsToTenant(projectId, tenantId);
        verify(users).ensureBelongsToTenant(assigneeId, tenantId);
    }

    @Test
    void unProyectoNoAsignadoSeReportaComoInexistente() {
        when(projects.isUserAssignedToProject(projectId, tenantId, authorId)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, author, request()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnResponsableDeOtroTenant() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(users).ensureBelongsToTenant(assigneeId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, projectId, author, request()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void elAutorPuedeEditarSinPermisoAdicional() {
        SiteLogRecord record = record(authorId, LocalDateTime.of(2026, 11, 27, 10, 0));
        when(repository.findById(record.id(), tenantId)).thenReturn(Optional.of(record));

        SiteLogRecordResponse response = service.update(
                tenantId, projectId, record.id(), author, request());

        assertEquals(SiteLogStatus.RESUELTO, response.status());
        verify(permissions, never()).requireCanManageSiteLog(any(), any());
    }

    @Test
    void otroUsuarioSinPermisoNoPuedeEditar() {
        SiteLogRecord record = record(authorId, LocalDateTime.of(2026, 11, 27, 10, 0));
        AuthenticatedUser other = new AuthenticatedUser(UUID.randomUUID(), tenantId, Role.WORKER);
        when(projects.isUserAssignedToProject(projectId, tenantId, other.userId())).thenReturn(true);
        when(repository.findById(record.id(), tenantId)).thenReturn(Optional.of(record));
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(permissions).requireCanManageSiteLog(tenantId, other.userId());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.update(tenantId, projectId, record.id(), other, request()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void touchCambiaLaModificacionPeroNoLaCreacion() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 20, 9, 0);
        SiteLogRecord record = record(authorId, created);
        when(repository.findById(record.id(), tenantId)).thenReturn(Optional.of(record));

        SiteLogRecordResponse response = service.touch(tenantId, projectId, record.id(), author);

        assertEquals(created, response.createdAt());
        assertNotEquals(created, response.updatedAt());
    }

    private SiteLogRecordRequest request() {
        return new SiteLogRecordRequest(SiteLogType.RETRASO, SiteLogStatus.RESUELTO,
                LocalDateTime.of(2026, 11, 27, 15, 30), "  Piso 3 - Torre A  ",
                "  Se colocó  ", "  Se registró el avance  ", "  Clima lluvioso  ", assigneeId);
    }

    private SiteLogRecord record(UUID author, LocalDateTime created) {
        return new SiteLogRecord(UUID.randomUUID(), tenantId, projectId, SiteLogType.AVANCE,
                SiteLogStatus.ABIERTO, "Avance", "Detalle", created.plusHours(1),
                "Piso 2", null, author, assigneeId, created, created);
    }

    private UserSummary summary(UUID id, String fullName) {
        return new UserSummary(id, "correo@prueba.com", fullName, Role.MEMBER, true);
    }
}
