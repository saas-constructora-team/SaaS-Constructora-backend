package com.SaaS.Constructora.shared.security;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Contexto del tenant (empresa) para la petición en curso.
 * <p>
 * El tenant se deriva del JWT ya validado y de la membresía activa del usuario,
 * nunca de una cabecera enviada por el cliente. Se expone mediante un
 * {@link ThreadLocal} para que cualquier capa pueda consultarlo sin propagar el
 * identificador manualmente.
 * <p>
 * El filtro que lo puebla es responsable de invocar {@link #clear()} en un
 * bloque {@code finally}; de lo contrario el hilo del pool podría reutilizar el
 * tenant de una petición anterior.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() { }

    /** Asocia la petición actual con un tenant. */
    public static void setCurrentTenantId(UUID tenantId) {
        CURRENT_TENANT_ID.set(Objects.requireNonNull(tenantId, "tenantId"));
    }

    /** Tenant de la petición actual, si ya fue resuelto. */
    public static Optional<UUID> getCurrentTenantId() {
        return Optional.ofNullable(CURRENT_TENANT_ID.get());
    }

    /** Limpia el contexto al terminar la petición. */
    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
