package com.SaaS.Constructora.projects.application.ports;

import java.util.UUID;

/**
 * Contrato público del módulo projects para validar que un proyecto pertenece
 * al tenant actual, sin exponer el servicio concreto ni la persistencia.
 */
public interface ProjectTenantPort {

    void ensureBelongsToTenant(UUID projectId, UUID tenantId);
}
