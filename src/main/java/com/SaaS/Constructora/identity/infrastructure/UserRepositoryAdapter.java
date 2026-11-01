package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.User;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;
    private final MembershipJpaRepository membershipRepository;

    UserRepositoryAdapter(UserJpaRepository repository, MembershipJpaRepository membershipRepository) {
        this.repository = repository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public List<User> findByTenantIdOrderByFullNameAsc(UUID tenantId) {
        // Buscar usuarios que tengan membresía activa en el tenant
        return membershipRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(m -> repository.findById(m.getUserId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(UserRepositoryAdapter::toDomain)
                .sorted(java.util.Comparator.comparing(User::getFullName))
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = repository.save(new UserJpaEntity(
                user.getId(), user.getEmail(), user.getFullName(),
                user.getPasswordHash(), user.isActive(), user.getCreatedAt()));
        return toDomain(saved);
    }

    @Override
    public void updatePasswordHash(UUID userId, String passwordHash) {
        repository.findById(userId).ifPresent(e -> {
            e.setPasswordHash(passwordHash);
            repository.save(e);
        });
    }

    private static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getEmail(), e.getFullName(),
                e.getPasswordHash(), e.isActive(), e.getCreatedAt());
    }
}
