package com.SaaS.Constructora.documents.application.ports;

import java.util.UUID;

/**
 * Contrato público de solo lectura del módulo documents. Permite a otros
 * módulos verificar que un documento existe dentro del tenant autenticado sin
 * consultar sus tablas ni conocer su persistencia.
 */
public interface DocumentTenantPort {

    boolean existsInTenant(UUID documentId, UUID tenantId);
}
