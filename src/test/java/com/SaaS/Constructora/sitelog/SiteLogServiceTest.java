package com.SaaS.Constructora.sitelog;

import com.SaaS.Constructora.documents.application.ports.DocumentTenantPort;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.sitelog.application.SiteLogService;
import com.SaaS.Constructora.sitelog.application.ports.SiteLogRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteLogServiceTest {

    private final SiteLogRepository repository = mock(SiteLogRepository.class);
    private final ProjectTenantPort projects = mock(ProjectTenantPort.class);
    private final DocumentTenantPort documents = mock(DocumentTenantPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final SiteLogService service = new SiteLogService(repository, projects, documents, events);

    @Test
    void asociaUnDocumentoDelTenantALaEtapaValidada() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(projects.isUserAssignedToProject(projectId, tenantId, userId)).thenReturn(true);
        when(documents.existsInTenant(documentId, tenantId)).thenReturn(true);
        when(repository.existsByDocumentIdAndTenantId(documentId, tenantId)).thenReturn(false);
        when(repository.save(any(SiteLogEntry.class))).thenAnswer(i -> i.getArgument(0));

        SiteLogEntry entry = service.associate(tenantId, projectId, stageId,
                userId, Role.MEMBER, documentId);

        assertEquals(tenantId, entry.tenantId());
        assertEquals(projectId, entry.projectId());
        assertEquals(stageId, entry.stageId());
        assertEquals(documentId, entry.documentId());
        assertEquals(userId, entry.createdBy());

        InOrder order = inOrder(projects, documents, repository);
        order.verify(projects).ensureBelongsToTenant(projectId, tenantId);
        order.verify(projects).isUserAssignedToProject(projectId, tenantId, userId);
        order.verify(projects).ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        order.verify(projects).ensureStageAllowsEntries(stageId);
        order.verify(documents).existsInTenant(documentId, tenantId);
        order.verify(repository).existsByDocumentIdAndTenantId(documentId, tenantId);
        order.verify(repository).save(any(SiteLogEntry.class));
    }

    @Test
    void unProyectoDeOtroTenantSeReportaComoInexistente() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(projects).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.associate(tenantId, projectId, UUID.randomUUID(),
                        UUID.randomUUID(), Role.ADMIN, UUID.randomUUID()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(documents, never()).existsInTenant(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaLaAsociacionEnUnaEtapaCerrada() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT))
                .when(projects).ensureStageAllowsEntries(stageId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.associate(tenantId, projectId, stageId,
                        UUID.randomUUID(), Role.ADMIN, documentId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(documents, never()).existsInTenant(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnDocumentoQueNoPerteneceAlTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documents.existsInTenant(documentId, tenantId)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.associate(tenantId, UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), Role.ADMIN, documentId));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaUnDocumentoYaAsociado() {
        UUID tenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(documents.existsInTenant(documentId, tenantId)).thenReturn(true);
        when(repository.existsByDocumentIdAndTenantId(documentId, tenantId)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.associate(tenantId, UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), Role.ADMIN, documentId));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void listaSoloLasAsociacionesDeLaEtapaDentroDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();
        SiteLogEntry entry = new SiteLogEntry(UUID.randomUUID(), tenantId, projectId, stageId,
                UUID.randomUUID(), UUID.randomUUID(), java.time.LocalDateTime.now());
        when(repository.findByTenantIdAndProjectIdAndStageId(tenantId, projectId, stageId))
                .thenReturn(List.of(entry));

        List<SiteLogEntry> result = service.list(tenantId, projectId, stageId,
                UUID.randomUUID(), Role.ADMIN);

        assertEquals(List.of(entry), result);
        verify(projects).ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        verify(projects, never()).ensureStageAllowsEntries(any());
    }
}
