package com.saas.constructora.modules.identity.interfaces;

import com.saas.constructora.modules.identity.application.UserQueryService;
import com.saas.constructora.modules.identity.interfaces.dto.UserResponse;
import com.saas.constructora.shared.security.TenantClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
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

    UserController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
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
}
