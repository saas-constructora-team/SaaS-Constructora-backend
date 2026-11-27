package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.application.UserSummary;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Contrato público y de solo lectura del módulo identity para consultar usuarios. */
public interface UserQueryPort {

    /** Valida que el usuario exista y pertenezca al tenant indicado (404 si no). */
    UserSummary ensureBelongsToTenant(UUID userId, UUID tenantId);

    List<UserSummary> listByTenant(UUID tenantId);

    List<UserSummary> listByTenant(UUID tenantId, Collection<UUID> userIds);
}
