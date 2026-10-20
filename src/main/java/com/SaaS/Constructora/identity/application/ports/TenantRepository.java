package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.Tenant;

/** Puerto (interfaz) para la persistencia de tenants; la implementación vive en infrastructure. */
public interface TenantRepository {
    boolean existsBySlug(String slug);

    Tenant save(Tenant tenant);
}
