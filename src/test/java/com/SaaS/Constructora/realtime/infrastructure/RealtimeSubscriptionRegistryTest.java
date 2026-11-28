package com.SaaS.Constructora.realtime.infrastructure;

import com.SaaS.Constructora.shared.events.ProjectChanged;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** El canal SSE solo entrega avisos a los suscriptores autorizados del tenant. */
class RealtimeSubscriptionRegistryTest {

    private final RealtimeSubscriptionRegistry registry = new RealtimeSubscriptionRegistry();
    private final UUID tenantId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Test
    void publicaSoloAlSuscriptorDelMismoProyectoYTenant() throws Exception {
        SseEmitter sameProject = mock(SseEmitter.class);
        SseEmitter otherProject = mock(SseEmitter.class);
        SseEmitter otherTenant = mock(SseEmitter.class);
        registry.subscribe(tenantId, projectId, sameProject);
        registry.subscribe(tenantId, UUID.randomUUID(), otherProject);
        registry.subscribe(UUID.randomUUID(), projectId, otherTenant);
        clearInvocations(sameProject, otherProject, otherTenant);

        registry.publish(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.PROGRESS, projectId, UUID.randomUUID()));

        verify(sameProject).send(any(SseEmitter.SseEventBuilder.class));
        verify(otherProject, never()).send(any(SseEmitter.SseEventBuilder.class));
        verify(otherTenant, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void elSuscriptorDeTodoElTenantRecibeCualquierProyecto() throws Exception {
        SseEmitter tenantWide = mock(SseEmitter.class);
        registry.subscribe(tenantId, null, tenantWide);
        clearInvocations(tenantWide);

        registry.publish(ProjectChanged.of(tenantId, UUID.randomUUID(),
                ProjectChanged.SITE_LOG, UUID.randomUUID(), UUID.randomUUID()));

        verify(tenantWide).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void elStreamTieneTimeoutFinitoParaNoRetenerConexiones() {
        SseEmitter emitter = registry.subscribe(tenantId, null);
        try {
            assertEquals(60_000L, emitter.getTimeout());
        } finally {
            emitter.complete();
        }
    }
}
