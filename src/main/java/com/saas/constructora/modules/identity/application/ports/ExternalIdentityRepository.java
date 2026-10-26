package com.saas.constructora.modules.identity.application.ports;

import com.saas.constructora.modules.identity.domain.ExternalIdentity;
import java.util.Optional;

/** Puerto (interfaz) para la persistencia de identidades federadas (HU-01). */
public interface ExternalIdentityRepository {

    Optional<ExternalIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    ExternalIdentity save(ExternalIdentity identity);
}
