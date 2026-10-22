package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.Tenant;
import java.util.Optional;

/** Puerto (interfaz) para la persistencia de tenants; la implementación vive en infrastructure. */
public interface TenantRepository {
    boolean existsBySlug(String slug);

    /** Búsqueda por id (HU-01: la respuesta de login incluye el tenant del usuario). */
    Optional<Tenant> findById(java.util.UUID id);

    Tenant save(Tenant tenant);
}
