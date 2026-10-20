package com.SaaS.Constructora.identity.application.ports;

import com.SaaS.Constructora.identity.domain.User;
import java.util.Optional;

/** Puerto (interfaz) que el caso de uso necesita; la implementación vive en infrastructure. */
public interface UserRepository {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
