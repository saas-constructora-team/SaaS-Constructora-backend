package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto (interfaz) que el caso de uso necesita; la implementación vive en infrastructure. */
public interface UserRepository {
    Optional<User> findByEmail(String email);

    /** Usuarios con membresía activa, en una sola consulta, ordenados por nombre. */
    List<UserSummary> findActiveByTenantId(UUID tenantId);

    /** Igual que el método anterior, limitado a los ids solicitados. */
    List<UserSummary> findActiveByTenantIdAndUserIdIn(UUID tenantId, Collection<UUID> userIds);

    /** Búsqueda por id (HU-01: login con Google). */
    Optional<User> findById(java.util.UUID id);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID userId);

    User save(User user);

    /** Actualiza únicamente el hash de contraseña del usuario indicado (HU-13). */
    void updatePasswordHash(java.util.UUID userId, String passwordHash);

    /**
     * Fija la contraseña de una cuenta creada como invitada y la activa.
     * Es el paso de "claim" al aceptar una invitación.
     */
    void updatePasswordHashAndActivate(java.util.UUID userId, String passwordHash);
}
