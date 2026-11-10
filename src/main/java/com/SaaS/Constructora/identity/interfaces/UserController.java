package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.PermissionManagementUseCase;
import com.SaaS.Constructora.identity.application.UserQueryService;
import com.SaaS.Constructora.identity.interfaces.dto.UserResponse;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import com.SaaS.Constructora.shared.security.TenantClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Listado de usuarios del tenant autenticado. Solo administradores pueden
 * consultarlo, ya que su uso principal es elegir a quién asignar un proyecto.
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
    List<UserResponse> list(@AuthenticationPrincipal Jwt jwt) {
        TenantClaims.requireAdmin(jwt);
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        return userQueryService.listByTenant(tenantId)
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
}
