package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Puerta de entrada pública del módulo identity para que otros módulos validen
 * si un usuario puede crear tareas en su empresa.
 * <p>
 * El permiso efectivo lo resuelve {@link Membership}: un ADMIN siempre puede y
 * un WORKER o MEMBER solo si un ADMIN se lo concedió explícitamente. La decisión
 * se toma contra la base de datos para que un cambio de permiso aplique de
 * inmediato, sin esperar a que el usuario vuelva a iniciar sesión.
 */
@Service
public class TaskCreationPermissionService {

    private final MembershipRepository membershipRepository;

    public TaskCreationPermissionService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public void requireCanCreateTasks(UUID tenantId, UUID userId) {
        Membership membership = membershipRepository.findActiveByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(TaskCreationPermissionService::forbidden);

        if (!membership.canCreateTasks()) {
            throw forbidden();
        }
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tiene permisos para crear tareas");
    }
}
