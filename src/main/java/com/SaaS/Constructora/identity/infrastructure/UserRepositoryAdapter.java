package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.MembershipStatus;
import com.SaaS.Constructora.identity.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository repository;

    UserRepositoryAdapter(UserJpaRepository repository) {
        this.repository = repository;
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
    public List<UserSummary> findActiveByTenantId(UUID tenantId) {
        return repository.findActiveByTenantId(tenantId, MembershipStatus.ACTIVE)
                .stream()
                .map(UserRepositoryAdapter::toSummary)
                .toList();
    }

    @Override
    public List<UserSummary> findActiveByTenantIdAndUserIdIn(UUID tenantId, Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findActiveByTenantIdAndUserIdIn(
                        tenantId, MembershipStatus.ACTIVE, List.copyOf(userIds))
                .stream()
                .map(UserRepositoryAdapter::toSummary)
                .toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsernameIgnoreCase(String username) {
        return username != null && repository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID userId) {
        return username != null && repository.existsByUsernameIgnoreCaseAndIdNot(username, userId);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = repository.findById(user.getId())
                .orElseGet(() -> new UserJpaEntity(
                        user.getId(), user.getEmail(), user.getFullName(), user.getUsername(),
                        user.getPasswordHash(), user.isActive(), user.getCreatedAt()));
        entity.setFullName(user.getFullName());
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setActive(user.isActive());
        return toDomain(repository.save(entity));
    }

    @Override
    public void updatePasswordHash(UUID userId, String passwordHash) {
        repository.findById(userId).ifPresent(e -> {
            e.setPasswordHash(passwordHash);
            repository.save(e);
        });
    }

    @Override
    public void updatePasswordHashAndActivate(UUID userId, String passwordHash) {
        repository.findById(userId).ifPresent(e -> {
            e.setPasswordHash(passwordHash);
            e.setActive(true);
            repository.save(e);
        });
    }

    private static User toDomain(UserJpaEntity e) {
        return new User(e.getId(), e.getEmail(), e.getFullName(), e.getUsername(),
                e.getPasswordHash(), e.isActive(), e.getCreatedAt());
    }

    private static UserSummary toSummary(ActiveUserProjection p) {
        return new UserSummary(p.getId(), p.getEmail(), p.getFullName(), p.getUsername(), p.getRole(), p.isActive(),
                p.getRole().canCreatePersonnel() || p.getCanCreatePersonnel(),
                p.getRole().canViewAssignmentCandidates() || p.getCanViewAssignmentCandidates(),
                p.getRole().canCreateTasks() || p.getCanCreateTasks(),
                p.getRole().canManageSiteLog() || p.getCanManageSiteLog());
    }
}
