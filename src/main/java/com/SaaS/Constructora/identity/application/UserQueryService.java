package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Puerta de entrada pública del módulo identity para que otros módulos
 * consulten usuarios sin acceder a su repositorio ni a su tabla.
 */
@Service
public class UserQueryService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public UserQueryService(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * Valida que el usuario exista y pertenezca al tenant indicado.
     * Un usuario de otra empresa se reporta como inexistente (404) para no
     * revelar datos de otros tenants.
     */
    @Transactional(readOnly = true)
    public UserSummary ensureBelongsToTenant(UUID userId, UUID tenantId) {
        if (userId == null || tenantId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Verificar que el usuario tiene membresía activa en el tenant
        Membership membership = membershipRepository.findActiveByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        return UserSummary.from(user, membership);
    }

    /** Usuarios de la empresa, ordenados por nombre, para elegir a quién asignar. */
    @Transactional(readOnly = true)
    public List<UserSummary> listByTenant(UUID tenantId) {
        return userRepository.findByTenantIdOrderByFullNameAsc(tenantId)
                .stream()
                .map(user -> {
                    Membership membership = membershipRepository.findActiveByUserIdAndTenantId(user.getId(), tenantId)
                            .orElseThrow(() -> new IllegalStateException("Usuario sin membresía en su tenant"));
                    return UserSummary.from(user, membership);
                })
                .toList();
    }
}
