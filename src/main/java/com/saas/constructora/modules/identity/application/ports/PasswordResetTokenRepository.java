package com.saas.constructora.modules.identity.application.ports;

import com.saas.constructora.modules.identity.domain.PasswordResetToken;
import java.util.Optional;

/** Puerto de persistencia de tokens de recuperación (HU-13). */
public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Marca como usados los tokens vigentes previos de un usuario (evita reutilizaciones). */
    void invalidateActiveTokensForUser(java.util.UUID userId);
}
