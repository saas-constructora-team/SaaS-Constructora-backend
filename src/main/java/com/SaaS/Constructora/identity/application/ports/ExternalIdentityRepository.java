package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.ExternalIdentity;
import java.util.Optional;

/** Puerto (interfaz) para la persistencia de identidades federadas (HU-01). */
public interface ExternalIdentityRepository {

    Optional<ExternalIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    ExternalIdentity save(ExternalIdentity identity);
}
