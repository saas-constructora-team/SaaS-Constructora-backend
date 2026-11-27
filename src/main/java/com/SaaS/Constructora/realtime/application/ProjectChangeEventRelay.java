package com.SaaS.Constructora.realtime.application;

import com.SaaS.Constructora.realtime.infrastructure.RealtimeSubscriptionRegistry;
import com.SaaS.Constructora.shared.events.DocumentVersionUploaded;
import com.SaaS.Constructora.shared.events.ProjectAssignmentChanged;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Reenvía al canal SSE, después del commit, los cambios que ya existen como
 * eventos de integración. No publica nada nuevo: solo traduce metadatos.
 */
@Component
public class ProjectChangeEventRelay {

    private final RealtimeSubscriptionRegistry registry;

    public ProjectChangeEventRelay(RealtimeSubscriptionRegistry registry) {
        this.registry = registry;
    }

    @ApplicationModuleListener
    public void on(ProjectChanged event) {
        registry.publish(event);
    }

    @ApplicationModuleListener
    public void on(ProjectCreated event) {
        registry.publish(ProjectChanged.of(event.tenantId(), event.projectId(),
                ProjectChanged.PROJECT, event.projectId(), event.actorUserId()));
    }

    @ApplicationModuleListener
    public void on(ProjectAssignmentChanged event) {
        registry.publish(ProjectChanged.of(event.tenantId(), event.projectId(),
                ProjectChanged.ASSIGNMENT, event.assignmentId(), event.actorUserId()));
    }

    @ApplicationModuleListener
    public void on(DocumentVersionUploaded event) {
        registry.publish(ProjectChanged.of(event.tenantId(), event.projectId(),
                ProjectChanged.DOCUMENT, event.documentId(), event.actorUserId()));
    }
}
