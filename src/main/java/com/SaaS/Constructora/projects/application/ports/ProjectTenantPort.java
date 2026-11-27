package com.SaaS.Constructora.projects.application.ports;

import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.domain.ProjectPhoto;

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

    /**
     * Valida que la etapa pertenezca al proyecto y al tenant indicados.
     * Otros módulos lo usan antes de asociar registros a una etapa, sin
     * consultar las tablas de projects.
     */
    void ensureStageBelongsToProjectAndTenant(UUID stageId, UUID projectId, UUID tenantId);

    /** Valida que la etapa esté abierta y por tanto permita registrar novedades. */
    void ensureStageAllowsEntries(UUID stageId);

    /**
     * Registra evidencia fotográfica de una etapa/proyecto. El binario se delega
     * al puerto de almacenamiento; aquí solo se persisten los metadatos y la
     * clave del objeto. La etapa se valida con este mismo contrato antes de subir.
     */
    ProjectPhoto registerPhoto(UUID tenantId, UUID projectId, UUID stageId,
                               UUID userId, Role role, FileUpload file);
}
