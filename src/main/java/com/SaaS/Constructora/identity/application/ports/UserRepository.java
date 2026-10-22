package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.User;
import java.util.Optional;

/** Puerto (interfaz) que el caso de uso necesita; la implementación vive en infrastructure. */
public interface UserRepository {
    Optional<User> findByEmail(String email);

    /** Búsqueda por id (HU-01: login con Google). */
    Optional<User> findById(java.util.UUID id);

    boolean existsByEmail(String email);

    User save(User user);

    /** Actualiza únicamente el hash de contraseña del usuario indicado (HU-13). */
    void updatePasswordHash(java.util.UUID userId, String passwordHash);
}
