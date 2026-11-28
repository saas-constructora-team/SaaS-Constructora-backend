package com.SaaS.Constructora.realtime.infrastructure;

import com.SaaS.Constructora.shared.events.ProjectChanged;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Suscripciones SSE en memoria, agrupadas por tenant. Solo reenvía metadatos
 * del cambio: el cliente vuelve a consultar los datos por sus endpoints, que
 * ya aplican el aislamiento y los permisos por proyecto.
 */
@Component
public class RealtimeSubscriptionRegistry {

    /** Los streams tienen un límite: si el cliente no se va, el canal se libera y reconecta. */
    private static final long TIMEOUT_MS = 60_000L;
    private static final long HEARTBEAT_MS = 25_000L;

    private final Map<UUID, Set<Subscription>> byTenant = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public SseEmitter subscribe(UUID tenantId, UUID projectId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Subscription subscription = subscribe(tenantId, projectId, emitter);

        ScheduledFuture<?> heartbeat = heartbeats.scheduleAtFixedRate(() -> {
            try {
                subscription.emitter().send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException exception) {
                remove(tenantId, subscription);
            }
        }, HEARTBEAT_MS, HEARTBEAT_MS, TimeUnit.MILLISECONDS);
        subscription.setHeartbeat(heartbeat);

        try {
            emitter.send(SseEmitter.event()
                    .name("ready")
                    .data("connected")
                    .reconnectTime(3000L));
        } catch (IOException | IllegalStateException exception) {
            remove(tenantId, subscription);
        }
        return emitter;
    }

    /** Registra la suscripción; el emitter lo crea el método público o la prueba. */
    Subscription subscribe(UUID tenantId, UUID projectId, SseEmitter emitter) {
        Subscription subscription = new Subscription(projectId, emitter);
        byTenant.computeIfAbsent(tenantId, key -> new CopyOnWriteArraySet<>()).add(subscription);

        Runnable remove = () -> remove(tenantId, subscription);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        return subscription;
    }

    public void publish(ProjectChanged event) {
        Set<Subscription> subscriptions = byTenant.get(event.tenantId());
        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }
        for (Subscription subscription : subscriptions) {
            if (subscription.projectId() != null
                    && !subscription.projectId().equals(event.projectId())) {
                continue;
            }
            try {
                subscription.emitter().send(SseEmitter.event()
                        .name("project-changed")
                        .data(event));
            } catch (IOException | IllegalStateException exception) {
                remove(event.tenantId(), subscription);
            }
        }
    }

    /** Solo visible para pruebas: número de suscripciones activas de un tenant. */
    int count(UUID tenantId) {
        Set<Subscription> subscriptions = byTenant.get(tenantId);
        return subscriptions == null ? 0 : subscriptions.size();
    }

    private void remove(UUID tenantId, Subscription subscription) {
        subscription.cancelHeartbeat();
        Set<Subscription> subscriptions = byTenant.get(tenantId);
        if (subscriptions == null) {
            return;
        }
        subscriptions.remove(subscription);
        if (subscriptions.isEmpty()) {
            byTenant.remove(tenantId, subscriptions);
        }
    }

    @PreDestroy
    void stopHeartbeats() {
        heartbeats.shutdownNow();
    }

    private static final class Subscription {

        private final UUID projectId;
        private final SseEmitter emitter;
        private ScheduledFuture<?> heartbeat;

        private Subscription(UUID projectId, SseEmitter emitter) {
            this.projectId = projectId;
            this.emitter = emitter;
        }

        private UUID projectId() {
            return projectId;
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private void setHeartbeat(ScheduledFuture<?> heartbeat) {
            this.heartbeat = heartbeat;
        }

        private void cancelHeartbeat() {
            if (heartbeat != null) {
                heartbeat.cancel(false);
                heartbeat = null;
            }
        }
    }
}
