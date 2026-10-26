package com.saas.constructora.modules.identity.application.ports;

import com.saas.constructora.modules.identity.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto (interfaz) que el caso de uso necesita; la implementación vive en infrastructure. */
public interface UserRepository {
    Optional<User> findByEmail(String email);

    /** Usuarios de una empresa, ordenados por nombre (listado para asignaciones). */
    List<User> findByTenantIdOrderByFullNameAsc(UUID tenantId);

    /** Búsqueda por id (HU-01: login con Google). */
    Optional<User> findById(java.util.UUID id);

    boolean existsByEmail(String email);

    User save(User user);

    /** Actualiza únicamente el hash de contraseña del usuario indicado (HU-13). */
    void updatePasswordHash(java.util.UUID userId, String passwordHash);
}
