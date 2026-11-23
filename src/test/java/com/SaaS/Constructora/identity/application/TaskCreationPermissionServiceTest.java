package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.MembershipStatus;
import com.SaaS.Constructora.identity.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskCreationPermissionServiceTest {

    private final MembershipRepository repository = mock(MembershipRepository.class);
    private final TaskCreationPermissionService service =
            new TaskCreationPermissionService(repository);

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void unAdminPuedeCrearTareasSinPermisoExplícito() {
        when(repository.findActiveByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(membership(Role.ADMIN, false)));

        assertDoesNotThrow(() -> service.requireCanCreateTasks(tenantId, userId));
    }

    @Test
    void unWorkerConPermisoPuedeCrearTareas() {
        when(repository.findActiveByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(membership(Role.WORKER, true)));

        assertDoesNotThrow(() -> service.requireCanCreateTasks(tenantId, userId));
    }

    @Test
    void unWorkerSinPermisoNoPuedeCrearTareas() {
        when(repository.findActiveByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(membership(Role.WORKER, false)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanCreateTasks(tenantId, userId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void sinMembresiaActivaNoPuedeCrearTareas() {
        when(repository.findActiveByUserIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.requireCanCreateTasks(tenantId, userId));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    private Membership membership(Role role, boolean taskCreationAllowed) {
        return new Membership(UUID.randomUUID(), userId, tenantId, role,
                MembershipStatus.ACTIVE, true, false, false,
                taskCreationAllowed, LocalDateTime.now());
    }
}
