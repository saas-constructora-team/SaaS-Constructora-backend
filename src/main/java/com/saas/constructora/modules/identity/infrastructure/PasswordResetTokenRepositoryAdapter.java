package com.saas.constructora.modules.identity.infrastructure;

import com.saas.constructora.modules.identity.application.ports.PasswordResetTokenRepository;
import com.saas.constructora.modules.identity.domain.PasswordResetToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository repository;

    PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity saved = repository.save(new PasswordResetTokenJpaEntity(
                token.getId(), token.getUserId(), token.getTokenHash(),
                token.getExpiresAt(), token.isUsed(), token.getCreatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(PasswordResetTokenRepositoryAdapter::toDomain);
    }

    @Override
    public void invalidateActiveTokensForUser(UUID userId) {
        repository.findByUserIdAndUsedFalse(userId).forEach(t -> {
            t.setUsed(true);
            repository.save(t);
        });
    }

    private static PasswordResetToken toDomain(PasswordResetTokenJpaEntity e) {
        return new PasswordResetToken(e.getId(), e.getUserId(), e.getTokenHash(),
                e.getExpiresAt(), e.isUsed(), e.getCreatedAt());
    }
}
