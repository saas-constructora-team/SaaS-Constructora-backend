package com.SaaS.Constructora.realtime.application;

import com.SaaS.Constructora.realtime.infrastructure.RealtimeSubscriptionRegistry;
import com.SaaS.Constructora.shared.events.DocumentVersionUploaded;
import com.SaaS.Constructora.shared.events.ProjectAssignmentChanged;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** El relay traduce eventos de integración existentes; no publica datos nuevos. */
class ProjectChangeEventRelayTest {

    private final RealtimeSubscriptionRegistry registry = mock(RealtimeSubscriptionRegistry.class);
    private final ProjectChangeEventRelay relay = new ProjectChangeEventRelay(registry);
    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @Test
    void reenviaElEventoCambioDeProyectoTalCual() {
        ProjectChanged event = ProjectChanged.of(tenantId, projectId,
                ProjectChanged.PROGRESS, projectId, actorId);

        relay.on(event);

        verify(registry).publish(event);
    }

    @Test
    void traduceProyectosNuevos() {
        relay.on(new ProjectCreated(UUID.randomUUID(), tenantId, projectId,
                actorId, "Obra nueva", LocalDateTime.now()));

        assertEquals(ProjectChanged.PROJECT, captured().section());
        assertEquals(projectId, captured().entityId());
    }

    @Test
    void traduceCambiosDeAsignacion() {
        UUID assignmentId = UUID.randomUUID();
        relay.on(new ProjectAssignmentChanged(UUID.randomUUID(), tenantId, projectId,
                assignmentId, UUID.randomUUID(), actorId, "CREATED", "Asignación creada",
                LocalDateTime.now()));

        assertEquals(ProjectChanged.ASSIGNMENT, captured().section());
        assertEquals(tenantId, captured().tenantId());
        assertEquals(projectId, captured().projectId());
        assertEquals(assignmentId, captured().entityId());
        assertEquals(actorId, captured().actorUserId());
    }

    @Test
    void traduceNuevasVersionesDeDocumento() {
        UUID documentId = UUID.randomUUID();
        relay.on(new DocumentVersionUploaded(UUID.randomUUID(), tenantId, projectId,
                documentId, UUID.randomUUID(), actorId, "v2", "plano.pdf", 1024,
                LocalDateTime.now()));

        assertEquals(ProjectChanged.DOCUMENT, captured().section());
        assertEquals(tenantId, captured().tenantId());
        assertEquals(projectId, captured().projectId());
        assertEquals(documentId, captured().entityId());
    }

    private ProjectChanged captured() {
        ArgumentCaptor<ProjectChanged> captor = ArgumentCaptor.forClass(ProjectChanged.class);
        verify(registry).publish(captor.capture());
        return captor.getValue();
    }
}
