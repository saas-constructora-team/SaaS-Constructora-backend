package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.User;
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

    /**
     * Fija la contraseña de una cuenta creada como invitada y la activa.
     * Es el paso de "claim" al aceptar una invitación.
     */
    void updatePasswordHashAndActivate(java.util.UUID userId, String passwordHash);
}
