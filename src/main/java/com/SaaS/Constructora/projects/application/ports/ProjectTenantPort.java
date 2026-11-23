package com.SaaS.Constructora.projects.application.ports;

import java.util.UUID;

/**
 * Contrato público del módulo projects para validar que un proyecto pertenece
 * al tenant actual, sin exponer el servicio concreto ni la persistencia.
 */
public interface ProjectTenantPort {

    void ensureBelongsToTenant(UUID projectId, UUID tenantId);

    /**
     * Indica si el usuario tiene una asignación activa al proyecto dentro del
     * tenant. Otros módulos lo usan para aplicar sus propias reglas de acceso
     * sin consultar las tablas de projects.
     */
    boolean isUserAssignedToProject(UUID projectId, UUID tenantId, UUID userId);
}
