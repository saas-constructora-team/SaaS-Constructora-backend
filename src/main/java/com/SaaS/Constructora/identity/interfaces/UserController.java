package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.PermissionManagementUseCase;
import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.interfaces.dto.UserResponse;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Listado de usuarios del tenant autenticado. Los ADMIN siempre pueden
 * consultarlo; un MEMBER solo si un ADMIN le habilitó explícitamente el
 * permiso de consultar candidatos a asignación.
 */
@RestController
@RequestMapping("/api/users")
class UserController {

    private final UserQueryService userQueryService;
    private final PermissionManagementUseCase permissionManagementUseCase;

    UserController(UserQueryService userQueryService,
                   PermissionManagementUseCase permissionManagementUseCase) {
        this.userQueryService = userQueryService;
        this.permissionManagementUseCase = permissionManagementUseCase;
    }

    @GetMapping
    List<UserResponse> list(@CurrentUser AuthenticatedUser user) {
        userQueryService.requireCanViewAssignmentCandidates(user.tenantId(), user.userId());
        return userQueryService.listByTenant(user.tenantId())
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    /** Habilita los permisos de administración del usuario indicado. */
    @PostMapping("/{userId}/permissions/enable")
    UserResponse enablePermissions(@CurrentUser AuthenticatedUser user,
                                   @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(permissionManagementUseCase.enable(user.tenantId(), userId));
    }

    /** Deshabilita los permisos de administración del usuario indicado. */
    @PostMapping("/{userId}/permissions/disable")
    UserResponse disablePermissions(@CurrentUser AuthenticatedUser user,
                                    @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(permissionManagementUseCase.disable(user.tenantId(), userId));
    }

    /** Habilita a un miembro para crear y gestionar el personal de la empresa. */
    @PostMapping("/{userId}/personnel-creation/enable")
    UserResponse enablePersonnelCreation(@CurrentUser AuthenticatedUser user,
                                         @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.enablePersonnelCreation(user.tenantId(), userId));
    }

    /** Revoca el permiso de creación y gestión de personal del usuario indicado. */
    @PostMapping("/{userId}/personnel-creation/disable")
    UserResponse disablePersonnelCreation(@CurrentUser AuthenticatedUser user,
                                          @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.disablePersonnelCreation(user.tenantId(), userId));
    }

    /** Habilita a un miembro para consultar candidatos a asignación de proyectos. */
    @PostMapping("/{userId}/assignment-candidates-view/enable")
    UserResponse enableAssignmentCandidatesView(@CurrentUser AuthenticatedUser user,
                                                @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.enableAssignmentCandidatesView(user.tenantId(), userId));
    }

    /** Revoca el permiso de consultar candidatos a asignación del usuario indicado. */
    @PostMapping("/{userId}/assignment-candidates-view/disable")
    UserResponse disableAssignmentCandidatesView(@CurrentUser AuthenticatedUser user,
                                                 @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.disableAssignmentCandidatesView(user.tenantId(), userId));
    }

    /** Habilita a un trabajador para crear tareas en los proyectos asignados. */
    @PostMapping("/{userId}/task-creation/enable")
    UserResponse enableTaskCreation(@CurrentUser AuthenticatedUser user,
                                    @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.enableTaskCreation(user.tenantId(), userId));
    }

    /** Revoca el permiso de creación de tareas del usuario indicado. */
    @PostMapping("/{userId}/task-creation/disable")
    UserResponse disableTaskCreation(@CurrentUser AuthenticatedUser user,
                                     @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.disableTaskCreation(user.tenantId(), userId));
    }

    /** Habilita a un WORKER o MEMBER para editar/eliminar entradas de la bitácora. */
    @PostMapping("/{userId}/site-log-management/enable")
    UserResponse enableSiteLogManagement(@CurrentUser AuthenticatedUser user,
                                         @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.enableSiteLogManagement(user.tenantId(), userId));
    }

    /** Revoca el permiso de gestionar la bitácora del usuario indicado. */
    @PostMapping("/{userId}/site-log-management/disable")
    UserResponse disableSiteLogManagement(@CurrentUser AuthenticatedUser user,
                                          @PathVariable UUID userId) {
        user.requireAdmin();
        return UserResponse.from(
                permissionManagementUseCase.disableSiteLogManagement(user.tenantId(), userId));
    }
}
